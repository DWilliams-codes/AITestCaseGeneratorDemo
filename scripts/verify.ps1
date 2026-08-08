[CmdletBinding()]
param(
    [switch]$HarnessOnly
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
    Write-Error "Required command 'python' was not found. Install it explicitly; this script does not modify the environment."
    exit 1
}

$arguments = @((Join-Path $repositoryRoot 'scripts/verify.py'))
if ($HarnessOnly) { $arguments += '--harness-only' }
& $python.Source @arguments
exit $LASTEXITCODE
