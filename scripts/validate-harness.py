#!/usr/bin/env python3
"""Validate TestForge AI orchestration and evaluation assets without live calls."""

from __future__ import annotations

import json
import re
import sys
import tomllib
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
ERRORS: list[str] = []

MANUAL_INPUT_FIELDS = {
    "title",
    "userStory",
    "businessRequirements",
    "assumptions",
    "acceptanceCriteria",
}
REQUEST_RECORD_COMPONENTS = MANUAL_INPUT_FIELDS | {"requirementId", "correlationId"}
CRITERION_INPUT_COMPONENTS = {"key", "description"}
AUTOMATION_DRAFT_COMPONENTS = {
    "name",
    "setupActions",
    "testActions",
    "cleanupActions",
    "parameters",
    "unresolvedPlaceholders",
    "suitabilityScore",
}
ENUM_SOURCE_PATHS = {
    "TestCaseCategory": "backend/src/main/java/com/testforge/testcase/domain/TestCaseCategory.java",
    "TestPriority": "backend/src/main/java/com/testforge/testcase/domain/TestPriority.java",
    "DataSensitivity": "backend/src/main/java/com/testforge/testcase/domain/DataSensitivity.java",
    "CoverageIntent": "backend/src/main/java/com/testforge/testcase/domain/CoverageIntent.java",
    "AmbiguityCategory": "backend/src/main/java/com/testforge/requirement/domain/AmbiguityCategory.java",
    "AmbiguitySeverity": "backend/src/main/java/com/testforge/requirement/domain/AmbiguitySeverity.java",
}

AGENT_PROFILE_ALLOWLIST = {
    "architect.toml": ("architect", "read-only", "gpt-5.6-sol", "high"),
    "builder.toml": ("builder", "workspace-write", "gpt-5.6-terra", "high"),
    "reviewer.toml": ("reviewer", "read-only", None, "high"),
}
CODEX_CONFIG_KEYS = {"model", "model_reasoning_effort", "agents"}
AGENTS_CONFIG_KEYS = {
    "enabled",
    "max_concurrent_threads_per_session",
    "default_subagent_model",
    "default_subagent_reasoning_effort",
}
PRIMARY_MODEL = "gpt-5.6-terra"
PRIMARY_REASONING_EFFORT = "medium"
MODEL_ROUTING_DOCUMENTATION_FILES = {
    "AGENTS.md",
    "docs/agents/AGENT_ROSTER.md",
    "docs/agents/WORKFLOW_AUDIT.md",
}
MODEL_ROUTING_DOCUMENTATION_TERMS = (
    "built-in read-only explorer",
    "$repository-audit",
    "Terra-medium default",
    "persistent explorer profile",
    "Max reasoning is never persisted",
    "selected manually only for exceptional work",
)
SKILL_ALLOWLIST = (
    "feature-delivery",
    "testforge-evaluation",
    "repository-audit",
    "ai-generation-evals",
    "quality-gate",
    "security-review",
)
PROTECTED_PLAN_IGNORE = "/docs/plans/testforce-ai-mvp-execplan.md"
MANUAL_008_ID = "manual-008-comprehensive-output-bounds"
MANUAL_008_SCHEMA_PATH = "backend/src/main/resources/prompts/test-generation-schema-v2.json"

# The fixture's labels are deliberately stable evidence names; schema paths keep
# the application-owned JSON schema as the source for their actual maxima.
MANUAL_008_BOUNDARIES = (
    ("testCases", "testCases", 25),
    ("summary.assumptions", "requirementSummary.assumptions", 30),
    ("ambiguities", "ambiguities", 50),
    ("preconditions", "testCases[].preconditions", 30),
    ("testData", "testCases[].testData", 30),
    ("steps", "testCases[].steps", 30),
    ("ACKeys", "testCases[].acceptanceCriteriaKeys", 50),
    ("stepNumber", "testCases[].steps[].stepNumber", 30),
    ("actor", "requirementSummary.actor", 4000),
    ("goal", "requirementSummary.goal", 4000),
    ("businessValue", "requirementSummary.businessValue", 4000),
    ("assumption[]", "requirementSummary.assumptions[]", 4000),
    ("ambiguity.description", "ambiguities[].description", 4000),
    ("suggestedQuestion", "ambiguities[].suggestedQuestion", 4000),
    ("title", "testCases[].title", 300),
    ("objective", "testCases[].objective", 4000),
    ("precondition[]", "testCases[].preconditions[]", 4000),
    ("data.name", "testCases[].testData[].name", 200),
    ("description", "testCases[].testData[].description", 2000),
    ("example", "testCases[].testData[].exampleValue", 1000),
    ("strategy", "testCases[].testData[].generationStrategy", 100),
    ("step.action", "testCases[].steps[].action", 4000),
    ("expected", "testCases[].steps[].expectedResult", 4000),
    ("reference", "testCases[].steps[].testDataReference", 1000),
    ("final", "testCases[].finalExpectedOutcome", 4000),
    ("ACKey[]", "testCases[].acceptanceCriteriaKeys[]", 20),
    ("rationale", "testCases[].rationale", 4000),
)
MANUAL_008_TEXT_FIELDS = tuple(label for label, _, _ in MANUAL_008_BOUNDARIES[8:])
MANUAL_008_TAXONOMY = (
    ("shell command", "Shell command"),
    ("destructive filesystem command", "Destructive filesystem command"),
    ("destructive SQL", "Destructive SQL"),
    ("network call", "Network call"),
    ("protocol URI", "Protocol URI"),
    ("active HTML/JS", "Active HTML/JS"),
)

REQUIRED_FILES = (
    ".gitignore",
    "AGENTS.md",
    "PLANS.md",
    ".codex/config.toml",
    ".codex/agents/architect.toml",
    ".codex/agents/builder.toml",
    ".codex/agents/reviewer.toml",
    ".agents/skills/feature-delivery/SKILL.md",
    ".agents/skills/feature-delivery/agents/openai.yaml",
    ".agents/skills/testforge-evaluation/SKILL.md",
    ".agents/skills/testforge-evaluation/agents/openai.yaml",
    ".agents/skills/repository-audit/SKILL.md",
    ".agents/skills/repository-audit/agents/openai.yaml",
    ".agents/skills/ai-generation-evals/SKILL.md",
    ".agents/skills/ai-generation-evals/agents/openai.yaml",
    ".agents/skills/quality-gate/SKILL.md",
    ".agents/skills/quality-gate/agents/openai.yaml",
    ".agents/skills/security-review/SKILL.md",
    ".agents/skills/security-review/agents/openai.yaml",
    "docs/PRODUCT.md",
    "docs/TESTING.md",
    "docs/PLANS.md",
    "docs/agents/AGENT_ROSTER.md",
    "docs/agents/WORKFLOW_AUDIT.md",
    "docs/product-specs/mvp-1-test-generation.md",
    "docs/decisions/README.md",
    "docs/exec-plans/README.md",
    "evals/manual-test-generation.jsonl",
    "evals/automation-generation.jsonl",
    "evals/RUBRIC.md",
    "scripts/verify.ps1",
    "scripts/verify.sh",
    ".github/pull_request_template.md",
)


def fail(location: str | Path, message: str) -> None:
    ERRORS.append(f"{location}: {message}")


def load_toml(relative_path: str) -> dict[str, Any]:
    path = ROOT / relative_path
    try:
        with path.open("rb") as handle:
            return tomllib.load(handle)
    except (OSError, tomllib.TOMLDecodeError) as error:
        fail(relative_path, f"cannot parse TOML ({error})")
        return {}


def require_nonblank_string(value: Any, location: str) -> bool:
    if not isinstance(value, str) or not value.strip():
        fail(location, "must be a nonblank string")
        return False
    return True


def model_routing_documentation_errors(text: str) -> list[str]:
    normalized = re.sub(r"\s+", " ", text)
    return [
        f"missing model-routing concept {term!r}"
        for term in MODEL_ROUTING_DOCUMENTATION_TERMS
        if term not in normalized
    ]


def discovery_errors(actual: set[str], expected: set[str], label: str) -> list[str]:
    errors: list[str] = []
    missing = sorted(expected - actual)
    unexpected = sorted(actual - expected)
    if missing:
        errors.append(f"missing {label}: {missing}")
    if unexpected:
        errors.append(f"unexpected {label}: {unexpected}")
    return errors


def duplicate_value_errors(values: dict[str, str], label: str) -> list[str]:
    locations_by_value: dict[str, list[str]] = {}
    for location, value in values.items():
        locations_by_value.setdefault(value, []).append(location)
    return [
        f"duplicate {label} {value!r} in {sorted(locations)}"
        for value, locations in sorted(locations_by_value.items())
        if len(locations) > 1
    ]


def validate_discovered_surface(
    location: str, actual: set[str], expected: set[str], label: str
) -> None:
    for message in discovery_errors(actual, expected, label):
        fail(location, message)


def is_external_or_escaping_config_file(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    normalized = value.replace("\\", "/")
    return (
        normalized.startswith("/")
        or re.match(r"^[A-Za-z]:/", normalized) is not None
        or ".." in normalized.split("/")
    )


def codex_config_structure_errors(config: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if set(config) != CODEX_CONFIG_KEYS:
        errors.append(
            f"must contain exactly top-level keys {sorted(CODEX_CONFIG_KEYS)}"
        )
    if config.get("model") != PRIMARY_MODEL:
        errors.append(f"top-level model must be {PRIMARY_MODEL!r}")
    if config.get("model_reasoning_effort") != PRIMARY_REASONING_EFFORT:
        errors.append(
            "top-level model_reasoning_effort must be "
            f"{PRIMARY_REASONING_EFFORT!r}"
        )
    agents_config = config.get("agents")
    if not isinstance(agents_config, dict):
        errors.append("[agents] must be a mapping")
        return errors
    actual_keys = set(agents_config)
    if actual_keys != AGENTS_CONFIG_KEYS:
        errors.append(
            "[agents] must contain exactly "
            f"{sorted(AGENTS_CONFIG_KEYS)}; got {sorted(actual_keys)}"
        )
    for key in sorted(actual_keys - AGENTS_CONFIG_KEYS):
        nested = agents_config.get(key)
        if isinstance(nested, dict) and "config_file" in nested:
            errors.append(f"nested agent role {key!r} with config_file is not allowed")
            if is_external_or_escaping_config_file(nested.get("config_file")):
                errors.append(
                    f"external or escaping config_file for nested role {key!r} is not allowed"
                )
    if agents_config.get("enabled") is not True:
        errors.append("agents.enabled must be true")
    if agents_config.get("max_concurrent_threads_per_session") != 3:
        errors.append("max_concurrent_threads_per_session must be 3")
    if agents_config.get("default_subagent_model") != PRIMARY_MODEL:
        errors.append(f"default_subagent_model must be {PRIMARY_MODEL!r}")
    if (
        agents_config.get("default_subagent_reasoning_effort")
        != PRIMARY_REASONING_EFFORT
    ):
        errors.append(
            "default_subagent_reasoning_effort must be "
            f"{PRIMARY_REASONING_EFFORT!r}"
        )
    return errors


def agent_profile_structure_errors(
    filename: str, agent: dict[str, Any]
) -> list[str]:
    errors: list[str] = []
    profile = AGENT_PROFILE_ALLOWLIST.get(filename)
    if profile is None:
        return [f"unexpected agent profile {filename!r}"]
    _, _, expected_model, expected_effort = profile
    allowed_keys = {
        "name",
        "description",
        "sandbox_mode",
        "developer_instructions",
        "model_reasoning_effort",
    }
    if expected_model is not None:
        allowed_keys.add("model")
    if set(agent) != allowed_keys:
        errors.append(f"must contain exactly {sorted(allowed_keys)}")
    if expected_model is None:
        if "model" in agent:
            errors.append("Reviewer must inherit its model and must not pin one")
    elif agent.get("model") != expected_model:
        errors.append(f"model must be {expected_model!r}")
    if agent.get("model_reasoning_effort") != expected_effort:
        errors.append(f"model_reasoning_effort must be {expected_effort!r}")
    return errors


def inspect_codex_config_text(text: str) -> list[str]:
    try:
        config = tomllib.loads(text)
    except tomllib.TOMLDecodeError as error:
        return [f"cannot parse TOML ({error})"]
    return codex_config_structure_errors(config)


def validate_required_files() -> None:
    for relative_path in REQUIRED_FILES:
        if not (ROOT / relative_path).is_file():
            fail(relative_path, "required file is missing")

    plan_files = list((ROOT / "docs/exec-plans/active").glob("TF-*.md"))
    plan_files += list((ROOT / "docs/exec-plans/completed").glob("TF-*.md"))
    if not plan_files:
        fail("docs/exec-plans", "at least one versioned TF ExecPlan is required")


def validate_agent_configuration() -> None:
    config = load_toml(".codex/config.toml")
    for message in codex_config_structure_errors(config):
        fail(".codex/config.toml", message)

    agents_dir = ROOT / ".codex/agents"
    discovered_files = {path.name for path in agents_dir.glob("*.toml")}
    validate_discovered_surface(
        ".codex/agents",
        discovered_files,
        set(AGENT_PROFILE_ALLOWLIST),
        "agent profiles",
    )

    discovered_names: dict[str, str] = {}
    for filename, (name, mode, _, _) in AGENT_PROFILE_ALLOWLIST.items():
        relative_path = f".codex/agents/{filename}"
        agent = load_toml(relative_path)
        for message in agent_profile_structure_errors(filename, agent):
            fail(relative_path, message)
        if agent.get("name") != name:
            fail(relative_path, f"name must be {name!r}")
        if isinstance(agent.get("name"), str):
            discovered_names[filename] = agent["name"]
        if agent.get("sandbox_mode") != mode:
            fail(relative_path, f"sandbox_mode must be {mode!r}")
        require_nonblank_string(agent.get("description"), f"{relative_path}:description")
        instructions = agent.get("developer_instructions")
        if require_nonblank_string(instructions, f"{relative_path}:developer_instructions"):
            lowered = instructions.lower()
            if "agents.md" not in lowered:
                fail(relative_path, "developer_instructions must require reading AGENTS.md")
            if name in {"architect", "reviewer"} and "do not modify" not in lowered:
                fail(relative_path, "read-only agent must explicitly prohibit modification")
            if name == "builder" and "only" not in lowered:
                fail(relative_path, "builder instructions must preserve single-writer scope")

    for message in duplicate_value_errors(discovered_names, "agent profile name"):
        fail(".codex/agents", message)


def strip_java_comments(source: str) -> str:
    without_blocks = re.sub(r"/\*.*?\*/", "", source, flags=re.DOTALL)
    return re.sub(r"//.*", "", without_blocks)


def parse_java_enum_constants(source: str, enum_name: str) -> set[str]:
    cleaned = strip_java_comments(source)
    match = re.search(rf"\benum\s+{re.escape(enum_name)}\s*\{{(.*?)\}}", cleaned, re.DOTALL)
    if not match:
        raise ValueError(f"cannot find enum {enum_name}")
    constants_block = match.group(1).split(";", 1)[0]
    constants: set[str] = set()
    for declaration in constants_block.split(","):
        declaration = declaration.strip()
        if not declaration:
            continue
        constant_match = re.fullmatch(r"([A-Z][A-Z0-9_]*)(?:\s*\([^)]*\))?", declaration)
        if not constant_match:
            raise ValueError(f"unsupported enum declaration {declaration!r}")
        constants.add(constant_match.group(1))
    if not constants:
        raise ValueError(f"enum {enum_name} contains no constants")
    return constants


def split_java_record_components(header: str) -> list[str]:
    components: list[str] = []
    start = 0
    angle_depth = 0
    parenthesis_depth = 0
    bracket_depth = 0
    for index, character in enumerate(header):
        if character == "<":
            angle_depth += 1
        elif character == ">":
            angle_depth -= 1
        elif character == "(":
            parenthesis_depth += 1
        elif character == ")":
            parenthesis_depth -= 1
        elif character == "[":
            bracket_depth += 1
        elif character == "]":
            bracket_depth -= 1
        elif character == "," and angle_depth == parenthesis_depth == bracket_depth == 0:
            components.append(header[start:index].strip())
            start = index + 1
    components.append(header[start:].strip())
    if angle_depth or parenthesis_depth or bracket_depth:
        raise ValueError("unbalanced record component declaration")
    return [component for component in components if component]


def parse_java_record_components(source: str, record_name: str) -> set[str]:
    cleaned = strip_java_comments(source)
    match = re.search(
        rf"\brecord\s+{re.escape(record_name)}\s*\((.*?)\)\s*\{{",
        cleaned,
        re.DOTALL,
    )
    if not match:
        raise ValueError(f"cannot find record {record_name}")
    names: set[str] = set()
    for declaration in split_java_record_components(match.group(1)):
        name_match = re.search(r"([A-Za-z_$][A-Za-z0-9_$]*)\s*$", declaration)
        if not name_match:
            raise ValueError(f"cannot parse record component {declaration!r}")
        name = name_match.group(1)
        if name in names:
            raise ValueError(f"duplicate record component {name!r}")
        names.add(name)
    if not names:
        raise ValueError(f"record {record_name} contains no components")
    return names


def require_matching_contract(
    location: str, label: str, actual: set[str], expected: set[str]
) -> None:
    if actual != expected:
        fail(
            location,
            f"{label} drifted: expected {sorted(expected)}, got {sorted(actual)}",
        )


def run_contract_parser_self_tests() -> None:
    enum_source = "public enum FixtureState { READY, COMPLETE }"
    record_source = (
        "public record FixtureRecord(String name, "
        "java.util.Map<String, java.util.List<String>> values) {}"
    )
    try:
        enum_expected = {"READY", "COMPLETE"}
        record_expected = {"name", "values"}
        if parse_java_enum_constants(enum_source, "FixtureState") != enum_expected:
            raise ValueError("baseline enum parser result did not match")
        if parse_java_record_components(record_source, "FixtureRecord") != record_expected:
            raise ValueError("baseline record parser result did not match")

        enum_removed = parse_java_enum_constants(
            enum_source.replace(", COMPLETE", ""), "FixtureState"
        )
        enum_renamed = parse_java_enum_constants(
            enum_source.replace("COMPLETE", "FINISHED"), "FixtureState"
        )
        record_removed = parse_java_record_components(
            record_source.replace(", java.util.Map<String, java.util.List<String>> values", ""),
            "FixtureRecord",
        )
        record_renamed = parse_java_record_components(
            record_source.replace("values", "entries"), "FixtureRecord"
        )
        if enum_removed == enum_expected or enum_renamed == enum_expected:
            raise ValueError("enum removal or rename did not create a contract mismatch")
        if record_removed == record_expected or record_renamed == record_expected:
            raise ValueError("record removal or rename did not create a contract mismatch")
    except ValueError as error:
        fail("scripts/validate-harness.py:self-test", str(error))


def run_workflow_package_self_tests() -> None:
    location = "scripts/validate-harness.py:workflow-self-test"
    expected = {"architect.toml", "builder.toml", "reviewer.toml"}
    missing = discovery_errors(
        {"architect.toml", "builder.toml"}, expected, "agent profiles"
    )
    renamed = discovery_errors(
        {"architect.toml", "builder.toml", "critic.toml"},
        expected,
        "agent profiles",
    )
    unexpected = discovery_errors(
        expected | {"scout.toml"}, expected, "agent profiles"
    )
    duplicates = duplicate_value_errors(
        {"one": "repository-audit", "two": "repository-audit"},
        "skill frontmatter name",
    )
    _, parser_problems = inspect_skill_frontmatter(
        "---\nname: sample\nname: duplicate\ndescription: sample skill\n---\n"
    )
    valid_config_text = (
        'model = "gpt-5.6-terra"\n'
        'model_reasoning_effort = "medium"\n'
        "[agents]\n"
        "enabled = true\n"
        "max_concurrent_threads_per_session = 3\n"
        'default_subagent_model = "gpt-5.6-terra"\n'
        'default_subagent_reasoning_effort = "medium"\n'
    )
    valid_config_problems = inspect_codex_config_text(valid_config_text)
    nested_role_problems = inspect_codex_config_text(
        valid_config_text
        + "[agents.scout]\n"
        "config_file = 'scout.toml'\n"
    )
    escaping_config_problems = inspect_codex_config_text(
        valid_config_text
        + "[agents.scout]\n"
        "config_file = '../outside.toml'\n"
    )
    posix_absolute_config_problems = inspect_codex_config_text(
        valid_config_text
        + "[agents.scout]\n"
        "config_file = '/outside.toml'\n"
    )
    windows_absolute_config_problems = inspect_codex_config_text(
        valid_config_text
        + "[agents.scout]\n"
        "config_file = 'C:/outside.toml'\n"
    )
    duplicate_toml_problems = inspect_codex_config_text(
        valid_config_text.replace("enabled = true\n", "enabled = true\nenabled = false\n")
    )
    generic_model_problems = inspect_codex_config_text(
        valid_config_text.replace("gpt-5.6-terra", "terra")
    )
    luna_model_problems = inspect_codex_config_text(
        valid_config_text.replace("gpt-5.6-terra", "gpt-5.6-luna", 1)
    )
    concurrency_problems = inspect_codex_config_text(
        valid_config_text.replace(
            "max_concurrent_threads_per_session = 3",
            "max_concurrent_threads_per_session = 4",
        )
    )
    max_effort_problems = inspect_codex_config_text(
        valid_config_text.replace(
            'model_reasoning_effort = "medium"',
            'model_reasoning_effort = "max"',
            1,
        )
    )
    valid_profiles = {
        "architect.toml": {
            "name": "architect",
            "description": "read-only architect",
            "sandbox_mode": "read-only",
            "model": "gpt-5.6-sol",
            "model_reasoning_effort": "high",
            "developer_instructions": "Read AGENTS.md and do not modify files.",
        },
        "builder.toml": {
            "name": "builder",
            "description": "sole builder",
            "sandbox_mode": "workspace-write",
            "model": "gpt-5.6-terra",
            "model_reasoning_effort": "high",
            "developer_instructions": "Read AGENTS.md and write only approved files.",
        },
        "reviewer.toml": {
            "name": "reviewer",
            "description": "read-only reviewer",
            "sandbox_mode": "read-only",
            "model_reasoning_effort": "high",
            "developer_instructions": "Read AGENTS.md and do not modify files.",
        },
    }
    for filename, profile in valid_profiles.items():
        for message in agent_profile_structure_errors(filename, profile):
            fail(location, f"valid {filename} mapping was rejected: {message}")
    generic_profile_problems = agent_profile_structure_errors(
        "architect.toml",
        {**valid_profiles["architect.toml"], "model": "sol"},
    )
    luna_profile_problems = agent_profile_structure_errors(
        "architect.toml",
        {**valid_profiles["architect.toml"], "model": "gpt-5.6-luna"},
    )
    ultra_effort_problems = agent_profile_structure_errors(
        "architect.toml",
        {**valid_profiles["architect.toml"], "model_reasoning_effort": "ultra"},
    )
    reviewer_pin_problems = agent_profile_structure_errors(
        "reviewer.toml",
        {**valid_profiles["reviewer.toml"], "model": "gpt-5.6-sol"},
    )
    valid_routing_documentation = ". ".join(MODEL_ROUTING_DOCUMENTATION_TERMS)
    valid_routing_documentation_problems = model_routing_documentation_errors(
        valid_routing_documentation
    )
    for term in MODEL_ROUTING_DOCUMENTATION_TERMS:
        missing_term_problems = model_routing_documentation_errors(
            valid_routing_documentation.replace(term, "")
        )
        if not any(term in message for message in missing_term_problems):
            fail(location, f"documentation negative case did not detect {term!r}")
    expectations = (
        (missing, "missing agent profiles"),
        (renamed, "missing agent profiles"),
        (renamed, "unexpected agent profiles"),
        (unexpected, "unexpected agent profiles"),
        (duplicates, "duplicate skill frontmatter name"),
        (parser_problems, "duplicate frontmatter field"),
        (nested_role_problems, "nested agent role"),
        (escaping_config_problems, "external or escaping config_file"),
        (posix_absolute_config_problems, "external or escaping config_file"),
        (windows_absolute_config_problems, "external or escaping config_file"),
        (duplicate_toml_problems, "cannot parse TOML"),
        (generic_model_problems, "top-level model must be"),
        (luna_model_problems, "top-level model must be"),
        (concurrency_problems, "max_concurrent_threads_per_session must be 3"),
        (max_effort_problems, "top-level model_reasoning_effort must be"),
        (generic_profile_problems, "model must be 'gpt-5.6-sol'"),
        (luna_profile_problems, "model must be 'gpt-5.6-sol'"),
        (ultra_effort_problems, "model_reasoning_effort must be 'high'"),
        (reviewer_pin_problems, "must not pin one"),
    )
    for messages, expected_fragment in expectations:
        if not any(expected_fragment in message for message in messages):
            fail(location, f"negative case did not detect {expected_fragment!r}")
    if valid_config_problems:
        fail(location, f"valid Codex mapping was rejected: {valid_config_problems}")
    if valid_routing_documentation_problems:
        fail(
            location,
            "valid model-routing documentation was rejected: "
            f"{valid_routing_documentation_problems}",
        )


def validate_source_contracts() -> dict[str, Any]:
    schema_path = "backend/src/main/resources/prompts/test-generation-schema-v2.json"
    try:
        schema = json.loads((ROOT / schema_path).read_text(encoding="utf-8"))
        properties = schema["properties"]
        summary_properties = properties["requirementSummary"]["properties"]
        ambiguity_properties = properties["ambiguities"]["items"]["properties"]
        case_properties = properties["testCases"]["items"]["properties"]
        data_properties = case_properties["testData"]["items"]["properties"]
        step_properties = case_properties["steps"]["items"]["properties"]
    except (OSError, json.JSONDecodeError, KeyError, TypeError) as error:
        fail(schema_path, f"cannot inspect generation schema ({error})")
        return {"enums": {}, "manual_input_fields": set(), "criterion_fields": set()}

    java_enums: dict[str, set[str]] = {}
    for enum_name, relative_path in ENUM_SOURCE_PATHS.items():
        try:
            source = (ROOT / relative_path).read_text(encoding="utf-8")
            java_enums[enum_name] = parse_java_enum_constants(source, enum_name)
        except (OSError, ValueError) as error:
            fail(relative_path, f"cannot inspect Java enum ({error})")
            java_enums[enum_name] = set()

    schema_enum_sets = {
        "AmbiguityCategory": [set(ambiguity_properties["category"]["enum"])],
        "AmbiguitySeverity": [set(ambiguity_properties["severity"]["enum"])],
        "TestCaseCategory": [set(case_properties["category"]["enum"])],
        "TestPriority": [
            set(case_properties["priority"]["enum"]),
            set(case_properties["riskLevel"]["enum"]),
        ],
        "CoverageIntent": [set(case_properties["coverageIntent"]["enum"])],
        "DataSensitivity": [set(data_properties["sensitivity"]["enum"])],
    }
    for enum_name, schema_sets in schema_enum_sets.items():
        for index, schema_values in enumerate(schema_sets, start=1):
            require_matching_contract(
                schema_path,
                f"{enum_name} Java/schema enum #{index}",
                schema_values,
                java_enums[enum_name],
            )

    request_path = "backend/src/main/java/com/testforge/generation/provider/TestGenerationRequest.java"
    request_text = (ROOT / request_path).read_text(encoding="utf-8")
    try:
        request_components = parse_java_record_components(
            request_text, "TestGenerationRequest"
        )
        criterion_components = parse_java_record_components(request_text, "CriterionInput")
        require_matching_contract(
            request_path,
            "TestGenerationRequest components",
            request_components,
            REQUEST_RECORD_COMPONENTS,
        )
        require_matching_contract(
            request_path,
            "CriterionInput components",
            criterion_components,
            CRITERION_INPUT_COMPONENTS,
        )
    except ValueError as error:
        fail(request_path, f"cannot inspect provider request records ({error})")
        criterion_components = set()

    provider_path = (
        "backend/src/main/java/com/testforge/generation/provider/"
        "OpenAiTestGenerationProvider.java"
    )
    provider_text = (ROOT / provider_path).read_text(encoding="utf-8")
    provider_visible_fields = set(
        re.findall(r'minimized\.(?:put|set)\("([A-Za-z][A-Za-z0-9]*)"', provider_text)
    )
    require_matching_contract(
        provider_path,
        "provider-visible request fields",
        provider_visible_fields,
        MANUAL_INPUT_FIELDS,
    )

    result_path = "backend/src/main/java/com/testforge/generation/provider/TestGenerationResult.java"
    result_text = (ROOT / result_path).read_text(encoding="utf-8")
    output_record_shapes = {
        "TestGenerationResult": set(properties) | {"usage"},
        "RequirementSummary": set(summary_properties),
        "GeneratedAmbiguity": set(ambiguity_properties),
        "GeneratedTestCase": set(case_properties),
        "GeneratedTestData": set(data_properties),
        "GeneratedStep": set(step_properties),
        "UsageMetadata": {"inputTokens", "outputTokens"},
    }
    for record_name, expected_components in output_record_shapes.items():
        try:
            actual_components = parse_java_record_components(result_text, record_name)
            require_matching_contract(
                result_path,
                f"{record_name} components",
                actual_components,
                expected_components,
            )
        except ValueError as error:
            fail(result_path, f"cannot inspect {record_name} ({error})")

    schema_object_shapes = {
        "root": schema,
        "requirementSummary": properties["requirementSummary"],
        "ambiguity": properties["ambiguities"]["items"],
        "testCase": properties["testCases"]["items"],
        "testData": case_properties["testData"]["items"],
        "step": case_properties["steps"]["items"],
    }
    for label, object_schema in schema_object_shapes.items():
        require_matching_contract(
            schema_path,
            f"{label} required properties",
            set(object_schema.get("required", [])),
            set(object_schema.get("properties", {})),
        )

    versions_path = (
        "backend/src/main/java/com/testforge/generation/application/"
        "GenerationContractVersions.java"
    )
    versions_text = (ROOT / versions_path).read_text(encoding="utf-8")
    if 'PROMPT = "manual-test-v2"' not in versions_text:
        fail(versions_path, "manual fixture promptVersion no longer matches release tuple")
    if 'SCHEMA = "manual-test-schema-v2"' not in versions_text:
        fail(versions_path, "schema resource no longer matches release tuple")
    if 'RESULT = "manual-test-result-v1"' not in versions_text:
        fail(versions_path, "manual fixture result contract no longer matches release tuple")
    if 'VALIDATOR = "manual-test-validator-v2"' not in versions_text:
        fail(versions_path, "manual fixture validator no longer matches release tuple")

    provider_path = (
        "backend/src/main/java/com/testforge/generation/provider/"
        "OpenAiTestGenerationProvider.java"
    )
    provider_text = (ROOT / provider_path).read_text(encoding="utf-8")
    if 'PROMPT_RESOURCE = "classpath:prompts/test-generation-v2.txt"' not in provider_text:
        fail(provider_path, "provider prompt resource no longer matches release tuple")

    rollback_prompt_path = "backend/src/main/resources/prompts/test-generation-v1.txt"
    if not (ROOT / rollback_prompt_path).is_file():
        fail(rollback_prompt_path, "rollback prompt must remain available")

    prompt_path = "backend/src/main/resources/prompts/test-generation-v2.txt"
    prompt_text = (ROOT / prompt_path).read_text(encoding="utf-8")
    for phrase in (
        "untrusted data",
        "internally decompose",
        "smallest coherent suite",
        "acceptance criterion",
        "structured JSON",
    ):
        if phrase not in prompt_text:
            fail(prompt_path, f"manual evaluation assumption drifted; missing {phrase!r}")

    return {
        "enums": java_enums,
        "manual_input_fields": provider_visible_fields,
        "criterion_fields": criterion_components,
        "automation_draft_components": AUTOMATION_DRAFT_COMPONENTS,
    }


def inspect_skill_frontmatter(text: str) -> tuple[dict[str, str], list[str]]:
    problems: list[str] = []
    match = re.match(r"\A---\s*\n(.*?)\n---\s*\n", text, re.DOTALL)
    if not match:
        return {}, ["must start with YAML frontmatter"]
    fields: dict[str, str] = {}
    for line in match.group(1).splitlines():
        key, separator, value = line.partition(":")
        if not separator:
            problems.append(f"invalid frontmatter line {line!r}")
            continue
        key = key.strip()
        if key in fields:
            problems.append(f"duplicate frontmatter field {key!r}")
        fields[key] = value.strip()
    if set(fields) != {"name", "description"}:
        problems.append("frontmatter must contain only name and description")
    if "TODO" in text:
        problems.append("skill contains an unresolved TODO")
    return fields, problems


def parse_skill_frontmatter(relative_path: str) -> dict[str, str]:
    text = (ROOT / relative_path).read_text(encoding="utf-8")
    fields, problems = inspect_skill_frontmatter(text)
    for problem in problems:
        fail(relative_path, problem)
    return fields


def parse_openai_yaml(relative_path: str) -> dict[str, str]:
    text = (ROOT / relative_path).read_text(encoding="utf-8")
    lines = [line for line in text.splitlines() if line.strip()]
    if not lines or lines[0] != "interface:":
        fail(relative_path, "must contain one top-level interface mapping")
        return {}
    fields: dict[str, str] = {}
    pattern = re.compile(r'^  ([a-z_]+): "(.*)"$')
    for line in lines[1:]:
        match = pattern.fullmatch(line)
        if not match:
            fail(relative_path, f"unsupported or unquoted metadata line {line!r}")
            continue
        fields[match.group(1)] = match.group(2)
    expected = {"display_name", "short_description", "default_prompt"}
    if set(fields) != expected:
        fail(relative_path, f"interface must contain exactly {sorted(expected)}")
    return fields


def validate_skills() -> None:
    skills_dir = ROOT / ".agents/skills"
    discovered_skills = {
        path.parent.name for path in skills_dir.glob("*/SKILL.md")
    }
    validate_discovered_surface(
        ".agents/skills", discovered_skills, set(SKILL_ALLOWLIST), "skills"
    )

    discovered_names: dict[str, str] = {}
    for name in SKILL_ALLOWLIST:
        skill_path = f".agents/skills/{name}/SKILL.md"
        metadata_path = f".agents/skills/{name}/agents/openai.yaml"
        frontmatter = parse_skill_frontmatter(skill_path)
        if frontmatter.get("name") != name:
            fail(skill_path, f"frontmatter name must be {name!r}")
        if isinstance(frontmatter.get("name"), str):
            discovered_names[name] = frontmatter["name"]
        require_nonblank_string(frontmatter.get("description"), f"{skill_path}:description")
        interface = parse_openai_yaml(metadata_path)
        display_name = interface.get("display_name")
        short_description = interface.get("short_description")
        default_prompt = interface.get("default_prompt")
        require_nonblank_string(display_name, f"{metadata_path}:display_name")
        if require_nonblank_string(
            short_description, f"{metadata_path}:short_description"
        ) and not 25 <= len(short_description) <= 64:
            fail(metadata_path, "short_description must be 25 to 64 characters")
        if require_nonblank_string(default_prompt, f"{metadata_path}:default_prompt"):
            if f"${name}" not in default_prompt:
                fail(metadata_path, f"default_prompt must explicitly mention ${name}")

    for message in duplicate_value_errors(discovered_names, "skill frontmatter name"):
        fail(".agents/skills", message)


def load_jsonl(relative_path: str) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    path = ROOT / relative_path
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError as error:
        fail(relative_path, f"cannot read JSONL ({error})")
        return records
    if any(not line.strip() for line in lines):
        fail(relative_path, "blank JSONL lines are not allowed")
    for line_number, line in enumerate(lines, start=1):
        try:
            value = json.loads(line)
        except json.JSONDecodeError as error:
            fail(f"{relative_path}:{line_number}", f"invalid JSON ({error.msg})")
            continue
        if not isinstance(value, dict):
            fail(f"{relative_path}:{line_number}", "record must be a JSON object")
            continue
        records.append(value)
    return records


def validate_string_list(value: Any, location: str, *, allow_empty: bool = False) -> list[str]:
    if not isinstance(value, list) or (not value and not allow_empty):
        fail(location, "must be a JSON array" + ("" if allow_empty else " with values"))
        return []
    for index, item in enumerate(value):
        require_nonblank_string(item, f"{location}[{index}]")
    return [item for item in value if isinstance(item, str)]


def normalized_obligation(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip().casefold()


def manual_008_expected_tuples() -> set[tuple[str, str]]:
    boundaries = {
        ("AC-1", f"{label}: exact maximum {maximum} is accepted.")
        for label, _, maximum in MANUAL_008_BOUNDARIES
    }
    boundaries |= {
        ("AC-2", f"{label}: first-over maximum {maximum + 1} is rejected before persistence.")
        for label, _, maximum in MANUAL_008_BOUNDARIES
    }
    fenced = {("AC-3", f"Fenced-code payload in {field} is rejected.") for field in MANUAL_008_TEXT_FIELDS}
    taxonomy = {("AC-3", f"{display} in step.action is rejected.") for _, display in MANUAL_008_TAXONOMY}
    enforcement = {
        ("AC-4", "Schema validation independently enforces curated bounds."),
        ("AC-4", "Semantic validation independently enforces curated bounds."),
    }
    expected = boundaries | fenced | taxonomy | enforcement
    assert len(expected) == 81
    return expected


def schema_bounds(schema: Any, path: str = "") -> dict[str, int]:
    found: dict[str, int] = {}
    if not isinstance(schema, dict):
        return found
    for keyword in ("maxItems", "maxLength", "maximum"):
        value = schema.get(keyword)
        if isinstance(value, int):
            found[path] = value
    properties = schema.get("properties")
    if isinstance(properties, dict):
        for name, child in properties.items():
            child_path = f"{path}.{name}" if path else name
            found.update(schema_bounds(child, child_path))
    if "items" in schema:
        found.update(schema_bounds(schema["items"], f"{path}[]"))
    return found


def schema_free_text_paths(schema: Any, path: str = "") -> set[str]:
    if not isinstance(schema, dict):
        return set()
    schema_type = schema.get("type")
    is_string = schema_type == "string" or (
        isinstance(schema_type, list) and "string" in schema_type
    )
    found = {path} if is_string and isinstance(schema.get("maxLength"), int) else set()
    properties = schema.get("properties")
    if isinstance(properties, dict):
        for name, child in properties.items():
            child_path = f"{path}.{name}" if path else name
            found |= schema_free_text_paths(child, child_path)
    if "items" in schema:
        found |= schema_free_text_paths(schema["items"], f"{path}[]")
    return found


def manual_008_oracle_errors(records: list[dict[str, Any]], schema: dict[str, Any]) -> list[str]:
    fixtures = [record for record in records if record.get("id") == MANUAL_008_ID]
    if len(fixtures) != 1:
        return ["must locate exactly one Manual-008 fixture"]
    curated_bounds = {path: maximum for _, path, maximum in MANUAL_008_BOUNDARIES}
    actual_bounds = schema_bounds(schema)
    if set(actual_bounds) - set(curated_bounds):
        return ["schema bounded path absent curated"]
    if set(curated_bounds) - set(actual_bounds):
        return ["curated path absent schema"]
    if any(actual_bounds[path] != maximum for path, maximum in curated_bounds.items()):
        return ["wrong max"]
    curated_text_paths = {path for _, path, _ in MANUAL_008_BOUNDARIES[8:]}
    if schema_free_text_paths(schema) != curated_text_paths:
        return ["free-text path mismatch"]
    items = fixtures[0].get("expectations", {}).get("atomicCoverageItems")
    if not isinstance(items, list):
        return ["unexpected/duplicate obligation"]
    actual = {
        (item.get("acceptanceCriteriaKey"), normalized_obligation(item.get("obligation", "")))
        for item in items
        if isinstance(item, dict) and isinstance(item.get("obligation"), str)
    }
    expected = {(key, normalized_obligation(obligation)) for key, obligation in manual_008_expected_tuples()}
    generic = any(
        isinstance(item, dict)
        and isinstance(item.get("obligation"), str)
        and "every provider-authored text field" in normalized_obligation(item["obligation"])
        for item in items
    )
    if generic:
        return ["generic authored-field safety"]
    if len(items) != len(actual) or actual - expected:
        return ["unexpected/duplicate obligation"]
    missing = expected - actual
    if any(key == "AC-1" for key, _ in missing):
        return ["missing exact"]
    if any(key == "AC-2" for key, _ in missing):
        return ["missing first-over"]
    fenced = {
        ("AC-3", normalized_obligation(f"Fenced-code payload in {field} is rejected."))
        for field in MANUAL_008_TEXT_FIELDS
    }
    if fenced & missing:
        return ["missing fenced field"]
    taxonomy = {
        ("AC-3", normalized_obligation(f"{display} in step.action is rejected."))
        for _, display in MANUAL_008_TAXONOMY
    }
    if taxonomy & missing:
        return ["missing taxonomy"]
    if missing:
        return ["unexpected/duplicate obligation"]
    return []


def run_manual_008_oracle_self_tests() -> None:
    schema = json.loads((ROOT / MANUAL_008_SCHEMA_PATH).read_text(encoding="utf-8"))
    complete_items = [
        {"acceptanceCriteriaKey": key, "obligation": obligation}
        for key, obligation in manual_008_expected_tuples()
    ]
    complete = {"id": MANUAL_008_ID, "expectations": {"atomicCoverageItems": complete_items}}

    def assert_exact_diagnostic(name: str, expected: str, record: dict[str, Any], candidate_schema: dict[str, Any] = schema) -> None:
        errors = manual_008_oracle_errors([record], candidate_schema)
        assert errors == [expected], f"{name}: {errors}"

    assert not manual_008_oracle_errors([complete], schema), "complete_81"
    missing_exact = json.loads(json.dumps(complete))
    missing_exact["expectations"]["atomicCoverageItems"] = [item for item in complete_items if item["acceptanceCriteriaKey"] != "AC-1"]
    assert_exact_diagnostic("missing_exact", "missing exact", missing_exact)
    missing_first_over = json.loads(json.dumps(complete))
    missing_first_over["expectations"]["atomicCoverageItems"] = [item for item in complete_items if item["acceptanceCriteriaKey"] != "AC-2"]
    assert_exact_diagnostic("missing_first_over", "missing first-over", missing_first_over)
    wrong_max_schema = json.loads(json.dumps(schema))
    wrong_max_schema["properties"]["testCases"]["maxItems"] = 24
    assert_exact_diagnostic("wrong_max", "wrong max", complete, wrong_max_schema)
    new_bounded_schema = json.loads(json.dumps(schema))
    new_bounded_schema["properties"]["newBounded"] = {"type": "string", "maxLength": 1}
    assert_exact_diagnostic("new_schema_bounded_path_absent_curated", "schema bounded path absent curated", complete, new_bounded_schema)
    missing_curated_schema = json.loads(json.dumps(schema))
    del missing_curated_schema["properties"]["testCases"]["maxItems"]
    assert_exact_diagnostic("curated_path_absent_schema", "curated path absent schema", complete, missing_curated_schema)
    missing_fenced = json.loads(json.dumps(complete))
    missing_fenced["expectations"]["atomicCoverageItems"] = [item for item in complete_items if item["obligation"] != f"Fenced-code payload in {MANUAL_008_TEXT_FIELDS[0]} is rejected."]
    assert_exact_diagnostic("missing_fenced_field", "missing fenced field", missing_fenced)
    generic_safety = json.loads(json.dumps(complete))
    generic_safety["expectations"]["atomicCoverageItems"][0] = {"acceptanceCriteriaKey": "AC-3", "obligation": "Fenced-code payload in every provider-authored text field is rejected."}
    assert_exact_diagnostic("generic_authored_field_safety", "generic authored-field safety", generic_safety)
    missing_taxonomy = json.loads(json.dumps(complete))
    missing_taxonomy["expectations"]["atomicCoverageItems"] = [item for item in complete_items if item["obligation"] != "Shell command in step.action is rejected."]
    assert_exact_diagnostic("missing_taxonomy", "missing taxonomy", missing_taxonomy)
    duplicate = json.loads(json.dumps(complete))
    duplicate["expectations"]["atomicCoverageItems"].append(complete_items[0])
    assert_exact_diagnostic("unexpected_duplicate_obligation", "unexpected/duplicate obligation", duplicate)
    print(
        "Manual-008 oracle self-tests passed: 81 obligations; "
        "missing_exact, missing_first_over, wrong_max, "
        "new_schema_bounded_path_absent_curated, curated_path_absent_schema, "
        "missing_fenced_field, generic_authored_field_safety, missing_taxonomy, "
        "and unexpected_duplicate_obligation."
    )


def evaluator_matrix_errors(expectations: dict[str, Any], cases: list[dict[str, Any]], evidence: list[dict[str, Any]]) -> list[str]:
    errors: list[str] = []
    if not 1 <= len(cases) <= 25 or not expectations["minimumTestCases"] <= len(cases) <= expectations["maximumTestCases"]:
        errors.append("case count")
    expected = {(item["acceptanceCriteriaKey"], item["obligation"]) for item in expectations["atomicCoverageItems"]}
    by_id = {case.get("id"): case for case in cases}
    observed: set[tuple[str, str]] = set()
    for item in evidence:
        pair = (item.get("acceptanceCriteriaKey"), item.get("obligation"))
        if pair not in expected or pair in observed:
            errors.append("unknown or duplicate evidence")
        observed.add(pair)
        case = by_id.get(item.get("caseId"))
        if not isinstance(case, dict):
            errors.append("unknown case")
            continue
        if case.get("coverageIntent") != "ACCEPTANCE_CRITERIA":
            errors.append("supporting evidence")
        if pair[0] not in case.get("acceptanceCriteriaKeys", []):
            errors.append("wrong AC binding")
        step = next((step for step in case.get("steps", []) if step.get("stepNumber") == item.get("stepNumber")), None)
        if not isinstance(step, dict) or not isinstance(step.get("expectedResult"), str) or not step["expectedResult"].strip() or not isinstance(case.get("finalExpectedOutcome"), str) or not case["finalExpectedOutcome"].strip() or not isinstance(item.get("explanation"), str) or not item["explanation"].strip():
            errors.append("missing observable direct evidence")
    if observed != expected:
        errors.append("missing evidence")
    if expectations["requiresMultiAcceptanceCriteriaCase"]:
        qualifying = [case for case in cases if case.get("coverageIntent") == "ACCEPTANCE_CRITERIA" and len(set(case.get("acceptanceCriteriaKeys", []))) >= 2]
        consolidated = any(
            set(case["acceptanceCriteriaKeys"]) <= {item.get("acceptanceCriteriaKey") for item in evidence if item.get("caseId") == case.get("id")}
            for case in qualifying
        )
        if not consolidated:
            errors.append("missing multi-AC evidence")
    return errors


def run_evaluator_matrix_self_tests() -> None:
    expectations = {"minimumTestCases": 1, "maximumTestCases": 2, "requiresMultiAcceptanceCriteriaCase": True, "atomicCoverageItems": [{"acceptanceCriteriaKey": "AC-1", "obligation": "Submit and observe one result."}, {"acceptanceCriteriaKey": "AC-2", "obligation": "Retry and observe no duplicate."}]}
    direct = {"id":"direct","coverageIntent":"ACCEPTANCE_CRITERIA","acceptanceCriteriaKeys":["AC-1","AC-2"],"steps":[{"stepNumber":1,"expectedResult":"One result exists."},{"stepNumber":2,"expectedResult":"No duplicate exists."}],"finalExpectedOutcome":"One result remains."}
    supporting = {**direct,"id":"supporting","coverageIntent":"SUPPORTING_EXPLORATORY"}
    evidence = [{"acceptanceCriteriaKey":"AC-1","obligation":"Submit and observe one result.","caseId":"direct","stepNumber":1,"explanation":"Direct first path."},{"acceptanceCriteriaKey":"AC-2","obligation":"Retry and observe no duplicate.","caseId":"direct","stepNumber":2,"explanation":"Direct retry path."}]
    def assert_diagnostic(name: str, expected: str, errors: list[str]) -> None:
        assert expected in errors, f"{name}: {errors}"
    assert not evaluator_matrix_errors(expectations,[direct],evidence) # valid_direct_multi_ac and min
    second = {**direct,"id":"second","acceptanceCriteriaKeys":["AC-1"]}
    assert not evaluator_matrix_errors({**expectations,"minimumTestCases":2},[direct,second],evidence) # max
    assert_diagnostic("below_min","case count",evaluator_matrix_errors(expectations,[],evidence))
    assert_diagnostic("above_max","case count",evaluator_matrix_errors({**expectations,"maximumTestCases":1},[direct,second],evidence))
    assert_diagnostic("missing_obligation","missing evidence",evaluator_matrix_errors(expectations,[direct],evidence[:1]))
    assert_diagnostic("duplicate_evidence","unknown or duplicate evidence",evaluator_matrix_errors(expectations,[direct],evidence+[evidence[0]]))
    assert_diagnostic("unknown_case","unknown case",evaluator_matrix_errors({**expectations,"requiresMultiAcceptanceCriteriaCase":False},[direct],[{**evidence[0],"caseId":"unknown"},evidence[1]]))
    assert_diagnostic("supporting_only_case","supporting evidence",evaluator_matrix_errors({**expectations,"requiresMultiAcceptanceCriteriaCase":False},[supporting],[{**item,"caseId":"supporting"} for item in evidence]))
    wrong = {**direct,"acceptanceCriteriaKeys":["AC-1"]}
    assert_diagnostic("wrong_ac_binding","wrong AC binding",evaluator_matrix_errors({**expectations,"requiresMultiAcceptanceCriteriaCase":False},[wrong],[evidence[0],{**evidence[1],"caseId":"direct"}]))
    assert_diagnostic("unknown_step","missing observable direct evidence",evaluator_matrix_errors(expectations,[direct],[{**evidence[0],"stepNumber":99},evidence[1]]))
    blank = {**direct,"steps":[{"stepNumber":1,"expectedResult":""},{"stepNumber":2,"expectedResult":"No duplicate exists."}]}
    assert_diagnostic("blank_observable","missing observable direct evidence",evaluator_matrix_errors(expectations,[blank],evidence))
    singles = [{**direct,"id":"one","acceptanceCriteriaKeys":["AC-1"]},{**direct,"id":"two","acceptanceCriteriaKeys":["AC-2"]}]
    single_evidence = [{**evidence[0],"caseId":"one"},{**evidence[1],"caseId":"two"}]
    assert_diagnostic("absent_consolidation","missing multi-AC evidence",evaluator_matrix_errors(expectations,singles,single_evidence))
    assert_diagnostic("supporting_multi_ac","missing multi-AC evidence",evaluator_matrix_errors(expectations,singles+[supporting],single_evidence))
    detached = {**direct,"id":"detached"}
    assert_diagnostic("detached_unrelated_multi_ac","missing multi-AC evidence",evaluator_matrix_errors(expectations,singles+[detached],single_evidence))
    later = {**direct,"id":"later"}
    assert not evaluator_matrix_errors(expectations,[{**direct,"id":"early","acceptanceCriteriaKeys":["AC-1"]},later],[{**evidence[0],"caseId":"later"},{**evidence[1],"caseId":"later"}]) # later_valid_direct_multi_ac


def validate_manual_evaluations(source_contracts: dict[str, Any]) -> None:
    relative_path = "evals/manual-test-generation.jsonl"
    input_keys = source_contracts.get("manual_input_fields", set())
    criterion_fields = source_contracts.get("criterion_fields", set())
    java_enums = source_contracts.get("enums", {})
    records = load_jsonl(relative_path)
    if len(records) != 8:
        fail(relative_path, "must contain exactly eight manual-generation cases")
    identifiers: set[str] = set()
    consolidation_fixture_count = 0
    for index, record in enumerate(records, start=1):
        location = f"{relative_path}:{index}"
        expected_keys = {
            "id",
            "mode",
            "blocking",
            "fixtureVersion",
            "promptVersion",
            "resultContractVersion",
            "schemaVersion",
            "validatorVersion",
            "input",
            "expectations",
        }
        if set(record) != expected_keys:
            fail(location, f"record keys must be exactly {sorted(expected_keys)}")
        identifier = record.get("id")
        if require_nonblank_string(identifier, f"{location}:id"):
            if identifier in identifiers:
                fail(location, f"duplicate id {identifier!r}")
            identifiers.add(identifier)
        if record.get("mode") != "manual-test-generation":
            fail(location, "mode must be manual-test-generation")
        if record.get("blocking") is not True:
            fail(location, "manual evaluation must be blocking")
        if record.get("fixtureVersion") != 2:
            fail(location, "fixtureVersion must be 2")
        if record.get("promptVersion") != "manual-test-v2":
            fail(location, "promptVersion must match GenerationService")
        if record.get("resultContractVersion") != "manual-test-result-v1":
            fail(location, "resultContractVersion must match the release tuple")
        if record.get("schemaVersion") != "manual-test-schema-v2":
            fail(location, "schemaVersion must match the release tuple")
        if record.get("validatorVersion") != "manual-test-validator-v2":
            fail(location, "validatorVersion must match the release tuple")

        input_value = record.get("input")
        if not isinstance(input_value, dict):
            fail(f"{location}:input", "must be an object")
            continue
        if set(input_value) != input_keys:
            fail(
                f"{location}:input",
                f"keys must match provider-visible fields {sorted(input_keys)}",
            )
        for field in input_keys - {"acceptanceCriteria"}:
            require_nonblank_string(input_value.get(field), f"{location}:input.{field}")
        criteria = input_value.get("acceptanceCriteria")
        criterion_keys: list[str] = []
        if not isinstance(criteria, list) or not 1 <= len(criteria) <= 50:
            fail(f"{location}:input.acceptanceCriteria", "must contain 1 to 50 criteria")
        else:
            for criterion_index, criterion in enumerate(criteria):
                criterion_location = f"{location}:input.acceptanceCriteria[{criterion_index}]"
                if not isinstance(criterion, dict) or set(criterion) != criterion_fields:
                    fail(
                        criterion_location,
                        f"must match CriterionInput fields {sorted(criterion_fields)}",
                    )
                    continue
                key = criterion.get("key")
                if not isinstance(key, str) or not re.fullmatch(r"AC-[1-9][0-9]*", key):
                    fail(criterion_location, "key must use the AC-# contract")
                else:
                    criterion_keys.append(key)
                require_nonblank_string(criterion.get("description"), criterion_location)
            if len(set(criterion_keys)) != len(criterion_keys):
                fail(location, "acceptance criterion keys must be unique")

        expectations = record.get("expectations")
        if not isinstance(expectations, dict):
            fail(f"{location}:expectations", "must be an object")
            continue
        expectation_keys = {
            "minimumTestCases",
            "maximumTestCases",
            "atomicCoverageItems",
            "requiresMultiAcceptanceCriteriaCase",
            "requiredAcceptanceCriteria",
            "requiredCategories",
            "requiredCoverageIntents",
            "requiredAmbiguityCategories",
            "mustCover",
            "forbiddenOutput",
        }
        if set(expectations) != expectation_keys:
            fail(
                f"{location}:expectations",
                f"keys must be exactly {sorted(expectation_keys)}",
            )
        minimum_cases = expectations.get("minimumTestCases")
        if not isinstance(minimum_cases, int) or not 1 <= minimum_cases <= 25:
            fail(f"{location}:minimumTestCases", "must be an integer from 1 through 25")
        maximum_cases = expectations.get("maximumTestCases")
        if (
            not isinstance(maximum_cases, int)
            or not isinstance(minimum_cases, int)
            or not minimum_cases <= maximum_cases <= 25
        ):
            fail(
                f"{location}:maximumTestCases",
                "must be an integer from minimumTestCases through 25",
            )
        required_keys = validate_string_list(
            expectations.get("requiredAcceptanceCriteria"),
            f"{location}:requiredAcceptanceCriteria",
        )
        if set(required_keys) != set(criterion_keys):
            fail(location, "requiredAcceptanceCriteria must include every supplied key")
        atomic_items = expectations.get("atomicCoverageItems")
        atomic_keys: list[str] = []
        normalized_obligations: set[str] = set()
        if not isinstance(atomic_items, list) or not atomic_items:
            fail(f"{location}:atomicCoverageItems", "must be a non-empty JSON array")
        else:
            for atomic_index, item in enumerate(atomic_items):
                atomic_location = f"{location}:atomicCoverageItems[{atomic_index}]"
                if not isinstance(item, dict) or set(item) != {
                    "acceptanceCriteriaKey",
                    "obligation",
                }:
                    fail(
                        atomic_location,
                        "must contain only acceptanceCriteriaKey and obligation",
                    )
                    continue
                key = item.get("acceptanceCriteriaKey")
                if not isinstance(key, str) or key not in criterion_keys:
                    fail(atomic_location, "must reference one supplied acceptance criterion key")
                else:
                    atomic_keys.append(key)
                obligation = item.get("obligation")
                require_nonblank_string(obligation, f"{atomic_location}:obligation")
                if isinstance(obligation, str):
                    normalized = re.sub(r"\s+", " ", obligation).strip().casefold()
                    if normalized in normalized_obligations:
                        fail(atomic_location, "atomic obligation must be unique after normalization")
                    normalized_obligations.add(normalized)
        if set(atomic_keys) != set(criterion_keys):
            fail(location, "atomicCoverageItems must realize every supplied acceptance criterion")
        requires_consolidation = expectations.get("requiresMultiAcceptanceCriteriaCase")
        if not isinstance(requires_consolidation, bool):
            fail(location, "requiresMultiAcceptanceCriteriaCase must be boolean")
        elif requires_consolidation:
            if len(set(criterion_keys)) < 2:
                fail(location, "requiresMultiAcceptanceCriteriaCase needs at least two supplied criteria")
            consolidation_fixture_count += 1
        categories = validate_string_list(
            expectations.get("requiredCategories"), f"{location}:requiredCategories"
        )
        if not set(categories) <= java_enums.get("TestCaseCategory", set()):
            fail(location, "requiredCategories contains a value absent from the Java/schema enum")
        coverage = validate_string_list(
            expectations.get("requiredCoverageIntents"),
            f"{location}:requiredCoverageIntents",
        )
        if not set(coverage) <= java_enums.get("CoverageIntent", set()):
            fail(
                location,
                "requiredCoverageIntents contains a value absent from the Java/schema enum",
            )
        ambiguities = validate_string_list(
            expectations.get("requiredAmbiguityCategories"),
            f"{location}:requiredAmbiguityCategories",
            allow_empty=True,
        )
        if not set(ambiguities) <= java_enums.get("AmbiguityCategory", set()):
            fail(
                location,
                "requiredAmbiguityCategories contains a value absent from the Java/schema enum",
            )
        validate_string_list(expectations.get("mustCover"), f"{location}:mustCover")
        validate_string_list(
            expectations.get("forbiddenOutput"), f"{location}:forbiddenOutput"
        )
    try:
        schema = json.loads((ROOT / MANUAL_008_SCHEMA_PATH).read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(MANUAL_008_SCHEMA_PATH, f"cannot load Manual-008 schema oracle ({error})")
    else:
        for message in manual_008_oracle_errors(records, schema):
            fail(relative_path, message)
    if consolidation_fixture_count < 1:
        fail(relative_path, "must include a multi-acceptance-criterion consolidation fixture")


def validate_automation_evaluations(source_contracts: dict[str, Any]) -> None:
    relative_path = "evals/automation-generation.jsonl"
    automation_components = source_contracts.get("automation_draft_components", set())
    records = load_jsonl(relative_path)
    if len(records) != 3:
        fail(relative_path, "must contain exactly three roadmap automation cases")
    identifiers: set[str] = set()
    for index, record in enumerate(records, start=1):
        location = f"{relative_path}:{index}"
        expected_keys = {
            "id",
            "mode",
            "blocking",
            "status",
            "fixtureVersion",
            "contract",
            "input",
            "expectations",
        }
        if set(record) != expected_keys:
            fail(location, f"record keys must be exactly {sorted(expected_keys)}")
        identifier = record.get("id")
        if require_nonblank_string(identifier, f"{location}:id"):
            if identifier in identifiers:
                fail(location, f"duplicate id {identifier!r}")
            identifiers.add(identifier)
        if record.get("mode") != "automation-generation-roadmap":
            fail(location, "mode must be automation-generation-roadmap")
        if record.get("blocking") is not False or record.get("status") != "roadmap":
            fail(location, "automation evaluations must remain non-blocking roadmap cases")
        if record.get("fixtureVersion") != 1:
            fail(location, "fixtureVersion must be 1")
        if record.get("contract") != "AutomationDraftGenerator":
            fail(location, "contract must name the Stage 2 extension point")
        input_value = record.get("input")
        if not isinstance(input_value, dict) or set(input_value) != {
            "approvedManualTestCase",
            "automationContext",
        }:
            fail(location, "input must contain approvedManualTestCase and automationContext")
            continue
        test_case = input_value.get("approvedManualTestCase")
        expected_case_keys = {
            "id",
            "testCaseKey",
            "title",
            "preconditions",
            "steps",
            "finalExpectedOutcome",
            "acceptanceCriteriaKeys",
        }
        if not isinstance(test_case, dict) or set(test_case) != expected_case_keys:
            fail(location, "approvedManualTestCase does not match its Stage 2 record shape")
        else:
            for field in ("id", "testCaseKey", "title", "finalExpectedOutcome"):
                require_nonblank_string(test_case.get(field), f"{location}:{field}")
            validate_string_list(
                test_case.get("preconditions"), f"{location}:preconditions", allow_empty=True
            )
            validate_string_list(
                test_case.get("acceptanceCriteriaKeys"),
                f"{location}:acceptanceCriteriaKeys",
            )
            steps = test_case.get("steps")
            if not isinstance(steps, list) or not steps:
                fail(location, "approved manual case must contain steps")
            else:
                for step_index, step in enumerate(steps, start=1):
                    if not isinstance(step, dict) or set(step) != {
                        "stepNumber",
                        "action",
                        "expectedResult",
                        "testDataReference",
                    }:
                        fail(location, "automation input step has an invalid shape")
                        continue
                    if step.get("stepNumber") != step_index:
                        fail(location, "automation input steps must be contiguous from one")
                    require_nonblank_string(step.get("action"), f"{location}:step.action")
                    require_nonblank_string(
                        step.get("expectedResult"), f"{location}:step.expectedResult"
                    )
        context = input_value.get("automationContext")
        context_keys = {
            "targetPlatform",
            "environmentMetadata",
            "selectorMappings",
            "reusableFunctions",
            "namingStandards",
            "authenticationFlow",
            "cleanupStandard",
        }
        if not isinstance(context, dict) or set(context) != context_keys:
            fail(location, "automationContext does not match its Stage 2 record shape")
        expectations = record.get("expectations")
        expectation_keys = {"requiredDraftSections", "mustPreserve", "mustNotDo"}
        if not isinstance(expectations, dict) or set(expectations) != expectation_keys:
            fail(location, f"expectations must contain exactly {sorted(expectation_keys)}")
        else:
            required_sections = validate_string_list(
                expectations.get("requiredDraftSections"),
                f"{location}:requiredDraftSections",
            )
            if set(required_sections) != automation_components:
                fail(location, "requiredDraftSections must match the roadmap fixture contract")
            validate_string_list(expectations.get("mustPreserve"), f"{location}:mustPreserve")
            must_not_do = validate_string_list(
                expectations.get("mustNotDo"), f"{location}:mustNotDo"
            )
            if not any("execute" in item.lower() for item in must_not_do):
                fail(location, "roadmap fixture must explicitly prohibit execution")


def validate_rubric() -> None:
    relative_path = "evals/RUBRIC.md"
    text = (ROOT / relative_path).read_text(encoding="utf-8")
    required_phrases = (
        "80 points out of 100",
        "no hard failures",
        "Contract and safety | 25",
        "Acceptance-criteria traceability | 25",
        "Test design quality | 20",
        "Risk and coverage | 15",
        "Reviewability and clarity | 15",
        "semantic duplicate cases without a distinct risk, condition, or path",
        "inclusive of fixture minimum",
        "every fixture atomic obligation",
        "Supporting evidence cannot satisfy",
        "multi-criterion direct case must substantiate every mapped key",
        "merely name the keys",
        "blocking: false",
    )
    for phrase in required_phrases:
        if phrase not in text:
            fail(relative_path, f"missing rubric invariant {phrase!r}")
    points = [25, 25, 20, 15, 15]
    if sum(points) != 100:
        fail(relative_path, "score categories must total 100")


def validate_documentation() -> None:
    required_terms = {
        "AGENTS.md": (
            "TestForge AI",
            "$feature-delivery",
            "$repository-audit",
            "$ai-generation-evals",
            "$testforge-evaluation",
            "$quality-gate",
            "$security-review",
            "CONFORMS",
            "APPROVE",
            "Model-routing overlay",
            "gpt-5.6-terra",
            "gpt-5.6-sol",
        ),
        "PLANS.md": (
            "docs/PLANS.md",
            "first repository write",
            "seven CI jobs",
            "Lead",
        ),
        "README.md": ("docs/PRODUCT.md", "scripts/verify"),
        "CONTRIBUTING.md": ("ExecPlan", "scripts/verify"),
        "docs/PRODUCT.md": ("Stage 1", "AutomationDraftGenerator"),
        "docs/TESTING.md": (
            "no provider call",
            "automation-generation.jsonl",
            "harness first",
            "exact published commit SHA",
        ),
        "docs/PLANS.md": (
            "active/",
            "completed/",
            "first repository write",
            "seven CI jobs",
            "CONFORMS",
            "APPROVE",
        ),
        "docs/agents/AGENT_ROSTER.md": (
            "Existing role matrix",
            "Nine-capability map",
            "Routing examples",
            "AI generation and evaluations",
            "Final integration review",
            "Model routing",
            "gpt-5.6-terra",
            "gpt-5.6-sol",
        ),
        "docs/agents/WORKFLOW_AUDIT.md": (
            "Actual repository and stack",
            "Existing package inventory",
            "Contradictions, overlaps, and missing capabilities",
            "Requested-package comparison",
            "Files added or changed and why",
            "seven existing CI jobs",
            "supply-chain",
            "Compose database host-port",
            "model-routing addendum",
            "gpt-5.6-terra",
            "gpt-5.6-sol",
            "No Luna",
        ),
        "docs/product-specs/mvp-1-test-generation.md": ("manual-test-v2", "Stage 1"),
        ".github/pull_request_template.md": ("ExecPlan", "Evaluation"),
    }
    for relative_path, terms in required_terms.items():
        path = ROOT / relative_path
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8")
        for term in terms:
            if term not in text:
                fail(relative_path, f"missing required documentation term {term!r}")
        if relative_path in MODEL_ROUTING_DOCUMENTATION_FILES:
            for message in model_routing_documentation_errors(text):
                fail(relative_path, message)

    markdown_files = [ROOT / relative_path for relative_path in required_terms]
    markdown_files.extend(
        [ROOT / "docs/exec-plans/README.md", ROOT / "docs/decisions/README.md"]
    )
    link_pattern = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")
    for path in markdown_files:
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8")
        for target in link_pattern.findall(text):
            target = target.strip().split("#", 1)[0]
            if not target or re.match(r"^[a-z]+://", target):
                continue
            resolved = (path.parent / target).resolve()
            try:
                resolved.relative_to(ROOT)
            except ValueError:
                fail(path.relative_to(ROOT), f"link escapes repository: {target}")
                continue
            if not resolved.exists():
                fail(path.relative_to(ROOT), f"broken local link: {target}")

    ignore_path = ROOT / ".gitignore"
    ignore_lines = ignore_path.read_text(encoding="utf-8").splitlines()
    if ignore_lines.count(PROTECTED_PLAN_IGNORE) != 1:
        fail(
            ".gitignore",
            f"must contain exactly one protection entry {PROTECTED_PLAN_IGNORE!r}",
        )


def main() -> int:
    validate_required_files()
    if ERRORS:
        print("TestForge harness validation failed:", file=sys.stderr)
        for error in ERRORS:
            print(f"- {error}", file=sys.stderr)
        return 1

    validate_agent_configuration()
    run_contract_parser_self_tests()
    run_workflow_package_self_tests()
    run_evaluator_matrix_self_tests()
    run_manual_008_oracle_self_tests()
    source_contracts = validate_source_contracts()
    validate_skills()
    validate_manual_evaluations(source_contracts)
    validate_automation_evaluations(source_contracts)
    validate_rubric()
    validate_documentation()

    if ERRORS:
        print("TestForge harness validation failed:", file=sys.stderr)
        for error in ERRORS:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "TestForge harness validation passed: 3 specialist profiles, 6 skills, "
        "8 blocking manual fixtures, and 3 non-blocking automation roadmap fixtures."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
