#!/usr/bin/env python3
"""Run TestForge's deterministic local verification without modifying the environment."""

from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
FRONTEND_SCRIPTS = (
    "test:audit-policy",
    "comments:check",
    "format:check",
    "lint",
    "typecheck",
    "test:coverage",
    "build",
)


class VerificationError(RuntimeError):
    """Reports a deterministic prerequisite or child-process failure."""


def require_command(name: str) -> str:
    """Return the discovered executable path or fail without installing it."""
    executable = shutil.which(name)
    if executable is None:
        raise VerificationError(
            f"Required command '{name}' was not found. Install it explicitly; "
            "this script does not modify the environment."
        )
    return executable


def run(command: list[str], *, cwd: Path = ROOT) -> None:
    """Run one verification command and preserve its exact nonzero exit status."""
    completed = subprocess.run(command, cwd=cwd, check=False)
    if completed.returncode:
        raise SystemExit(completed.returncode)


def command_output(command: list[str]) -> str:
    """Capture a prerequisite version string while preserving diagnostic stderr."""
    completed = subprocess.run(
        command,
        cwd=ROOT,
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if completed.returncode:
        raise SystemExit(completed.returncode)
    return completed.stdout.strip()


def parse_version(value: str) -> tuple[int, ...]:
    """Extract a numeric version tuple from a tool's stable version output."""
    match = re.search(r"(\d+)(?:\.(\d+))?(?:\.(\d+))?", value)
    if not match:
        raise VerificationError(f"Could not parse tool version from: {value}")
    return tuple(int(part) for part in match.groups(default="0"))


def validate_runtime() -> tuple[str, str, str, str]:
    """Validate the supported build tools without changing or downloading them."""
    java = require_command("java")
    maven = require_command("mvn")
    node = require_command("node")
    npm = require_command("npm")

    java_output = command_output([java, "-version"])
    java_match = re.search(r'version "(\d+)', java_output)
    if not java_match or int(java_match.group(1)) != 21:
        raise VerificationError(f"Java 21 is required; found: {java_output.splitlines()[0]}")

    node_version = parse_version(command_output([node, "--version"]))
    if node_version < (22, 22, 0):
        raise VerificationError(
            "Node 22.22.0 or newer is required; found "
            f"{'.'.join(str(part) for part in node_version)}."
        )
    if not (ROOT / "frontend" / "node_modules").is_dir():
        raise VerificationError(
            "frontend/node_modules is missing. Run npm ci explicitly before verification."
        )
    return java, maven, node, npm


def validate_frontend_dependency_tree(npm: str) -> None:
    """Fail closed when installed packages diverge from the reviewed manifest and lock."""
    run(
        [npm, "ls", "--all", "--json", "--loglevel=silent"],
        cwd=ROOT / "frontend",
    )


def verify(harness_only: bool) -> None:
    """Run the canonical harness-first backend/frontend verification sequence."""
    if sys.version_info < (3, 11):
        raise VerificationError(
            f"Python 3.11 or newer is required; found {sys.version.split()[0]}."
        )

    print("==> TestForge deterministic harness", flush=True)
    run([sys.executable, "scripts/validate-harness.py"])
    if harness_only:
        print("TestForge harness-only verification passed.")
        return

    _, maven, _, npm = validate_runtime()
    print("==> Frontend dependency tree", flush=True)
    validate_frontend_dependency_tree(npm)
    print("==> Backend verify", flush=True)
    run([maven, "--batch-mode", "--no-transfer-progress", "verify"], cwd=ROOT / "backend")

    print("==> Frontend verify", flush=True)
    for script in FRONTEND_SCRIPTS:
        run([npm, "run", script], cwd=ROOT / "frontend")
    print("TestForge repository verification passed.")


def parse_args(argv: list[str]) -> argparse.Namespace:
    """Accept the platform-neutral flag plus the legacy PowerShell alias."""
    parser = argparse.ArgumentParser()
    parser.add_argument("--harness-only", "-HarnessOnly", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    """Translate verification failures into concise process diagnostics."""
    try:
        verify(parse_args(sys.argv[1:] if argv is None else argv).harness_only)
    except VerificationError as error:
        print(str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
