[CmdletBinding()]
param(
    [switch]$HarnessOnly
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot

function Require-Command {
    param([Parameter(Mandatory)][string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found. Install it explicitly; this script does not modify the environment."
    }
}

Push-Location $repositoryRoot
try {
    Require-Command python
    $pythonVersion = [Version](& python -c "import sys; print('.'.join(map(str, sys.version_info[:3])))")
    if ($pythonVersion -lt [Version]'3.11') {
        throw "Python 3.11 or newer is required for tomllib; found $pythonVersion."
    }

    Write-Host '==> TestForge deterministic harness'
    & python scripts/validate-harness.py
    if ($LASTEXITCODE -ne 0) { throw 'Harness validation failed.' }

    if ($HarnessOnly) {
        Write-Host 'TestForge harness-only verification passed.'
        return
    }

    foreach ($command in @('java', 'mvn', 'node', 'npm')) {
        Require-Command $command
    }
    $javaVersionText = (& java -version 2>&1 | Select-Object -First 1) -join ''
    if ($javaVersionText -notmatch 'version "21(?:\.|\")') {
        throw "Java 21 is required; found: $javaVersionText"
    }
    $nodeVersion = [Version]((& node --version).TrimStart('v'))
    if ($nodeVersion -lt [Version]'22.12.0') {
        throw "Node 22.12.0 or newer is required; found $nodeVersion."
    }
    if (-not (Test-Path -LiteralPath 'frontend/node_modules' -PathType Container)) {
        throw 'frontend/node_modules is missing. Run npm ci explicitly before verification.'
    }

    Write-Host '==> Backend verify'
    Push-Location backend
    try {
        & mvn --batch-mode --no-transfer-progress verify
        if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
    }
    finally {
        Pop-Location
    }

    Write-Host '==> Frontend verify'
    Push-Location frontend
    try {
        foreach ($script in @('format:check', 'lint', 'typecheck', 'test:coverage', 'build')) {
            & npm run $script
            if ($LASTEXITCODE -ne 0) { throw "Frontend '$script' failed." }
        }
    }
    finally {
        Pop-Location
    }

    Write-Host 'TestForge repository verification passed.'
}
finally {
    Pop-Location
}
