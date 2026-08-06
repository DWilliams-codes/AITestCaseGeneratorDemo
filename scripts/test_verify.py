"""Unit tests for the cross-platform verification orchestrator."""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path
from unittest.mock import patch


MODULE_PATH = Path(__file__).with_name("verify.py")
SPEC = importlib.util.spec_from_file_location("testforge_verify", MODULE_PATH)
assert SPEC and SPEC.loader
verify_module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = verify_module
SPEC.loader.exec_module(verify_module)


class VerifyTest(unittest.TestCase):
    """Proves ordering, aliases, and failure propagation without running tools."""

    def test_harness_only_runs_nothing_after_harness(self) -> None:
        calls: list[tuple[list[str], Path]] = []

        def capture(command: list[str], *, cwd: Path = verify_module.ROOT) -> None:
            calls.append((command, cwd))

        with patch.object(verify_module, "run", side_effect=capture):
            verify_module.verify(True)

        self.assertEqual(
            calls,
            [([sys.executable, "scripts/validate-harness.py"], verify_module.ROOT)],
        )

    def test_full_sequence_is_defined_once(self) -> None:
        calls: list[tuple[list[str], Path]] = []

        def capture(command: list[str], *, cwd: Path = verify_module.ROOT) -> None:
            calls.append((command, cwd))

        with (
            patch.object(verify_module, "run", side_effect=capture),
            patch.object(
                verify_module,
                "validate_runtime",
                return_value=("java", "mvn", "node", "npm"),
            ),
        ):
            verify_module.verify(False)

        self.assertEqual(calls[0][0], [sys.executable, "scripts/validate-harness.py"])
        self.assertEqual(
            calls[1],
            (
                ["npm", "ls", "--all", "--json", "--loglevel=silent"],
                verify_module.ROOT / "frontend",
            ),
        )
        self.assertEqual(calls[2], (["mvn", "--batch-mode", "--no-transfer-progress", "verify"], verify_module.ROOT / "backend"))
        self.assertEqual(
            [command[-1] for command, _ in calls[3:]],
            list(verify_module.FRONTEND_SCRIPTS),
        )

    def test_both_harness_only_spellings_are_supported(self) -> None:
        self.assertTrue(verify_module.parse_args(["--harness-only"]).harness_only)
        self.assertTrue(verify_module.parse_args(["-HarnessOnly"]).harness_only)

    def test_child_exit_code_is_propagated(self) -> None:
        with patch.object(verify_module.subprocess, "run") as process:
            process.return_value.returncode = 17
            with self.assertRaises(SystemExit) as raised:
                verify_module.run(["example"])
        self.assertEqual(raised.exception.code, 17)

    def test_stale_installed_dependency_tree_fails_closed(self) -> None:
        with patch.object(verify_module.subprocess, "run") as process:
            process.return_value.returncode = 1
            with self.assertRaises(SystemExit) as raised:
                verify_module.validate_frontend_dependency_tree("npm")

        self.assertEqual(raised.exception.code, 1)
        process.assert_called_once_with(
            ["npm", "ls", "--all", "--json", "--loglevel=silent"],
            cwd=verify_module.ROOT / "frontend",
            check=False,
        )


if __name__ == "__main__":
    unittest.main()
