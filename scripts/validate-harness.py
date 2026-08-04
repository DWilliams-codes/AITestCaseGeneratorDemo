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
}

REQUIRED_FILES = (
    "AGENTS.md",
    ".codex/config.toml",
    ".codex/agents/architect.toml",
    ".codex/agents/builder.toml",
    ".codex/agents/reviewer.toml",
    ".agents/skills/feature-delivery/SKILL.md",
    ".agents/skills/feature-delivery/agents/openai.yaml",
    ".agents/skills/testforge-evaluation/SKILL.md",
    ".agents/skills/testforge-evaluation/agents/openai.yaml",
    "docs/PRODUCT.md",
    "docs/TESTING.md",
    "docs/PLANS.md",
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
    if set(config) != {"agents"}:
        fail(".codex/config.toml", "must contain only the [agents] table")
    agents_config = config.get("agents", {})
    if agents_config.get("enabled") is not True:
        fail(".codex/config.toml", "agents.enabled must be true")
    if agents_config.get("max_concurrent_threads_per_session") != 3:
        fail(
            ".codex/config.toml",
            "max_concurrent_threads_per_session must be 3 for the bounded workflow",
        )

    expected_modes = {
        "architect": "read-only",
        "builder": "workspace-write",
        "reviewer": "read-only",
    }
    for name, mode in expected_modes.items():
        relative_path = f".codex/agents/{name}.toml"
        agent = load_toml(relative_path)
        allowed_keys = {"name", "description", "sandbox_mode", "developer_instructions"}
        if set(agent) != allowed_keys:
            fail(relative_path, f"must contain exactly {sorted(allowed_keys)}")
        if agent.get("name") != name:
            fail(relative_path, f"name must be {name!r}")
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


def validate_source_contracts() -> dict[str, Any]:
    schema_path = "backend/src/main/resources/prompts/test-generation-schema-v1.json"
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

    service_path = "backend/src/main/java/com/testforge/generation/application/GenerationService.java"
    service_text = (ROOT / service_path).read_text(encoding="utf-8")
    if 'PROMPT_VERSION = "manual-test-v1"' not in service_text:
        fail(service_path, "manual fixture promptVersion no longer matches GenerationService")

    prompt_path = "backend/src/main/resources/prompts/test-generation-v1.txt"
    prompt_text = (ROOT / prompt_path).read_text(encoding="utf-8")
    for phrase in ("untrusted data", "acceptance criterion", "structured JSON"):
        if phrase not in prompt_text:
            fail(prompt_path, f"manual evaluation assumption drifted; missing {phrase!r}")

    automation_path = "backend/src/main/java/com/testforge/automation/AutomationDraft.java"
    automation_text = (ROOT / automation_path).read_text(encoding="utf-8")
    actual_automation_components: set[str] = set()
    try:
        actual_automation_components = parse_java_record_components(
            automation_text, "AutomationDraft"
        )
        require_matching_contract(
            automation_path,
            "AutomationDraft components",
            actual_automation_components,
            AUTOMATION_DRAFT_COMPONENTS,
        )
    except ValueError as error:
        fail(automation_path, f"cannot inspect AutomationDraft ({error})")

    return {
        "enums": java_enums,
        "manual_input_fields": provider_visible_fields,
        "criterion_fields": criterion_components,
        "automation_draft_components": actual_automation_components,
    }


def parse_skill_frontmatter(relative_path: str) -> dict[str, str]:
    text = (ROOT / relative_path).read_text(encoding="utf-8")
    match = re.match(r"\A---\s*\n(.*?)\n---\s*\n", text, re.DOTALL)
    if not match:
        fail(relative_path, "must start with YAML frontmatter")
        return {}
    fields: dict[str, str] = {}
    for line in match.group(1).splitlines():
        key, separator, value = line.partition(":")
        if not separator:
            fail(relative_path, f"invalid frontmatter line {line!r}")
            continue
        fields[key.strip()] = value.strip()
    if set(fields) != {"name", "description"}:
        fail(relative_path, "frontmatter must contain only name and description")
    if "TODO" in text:
        fail(relative_path, "skill contains an unresolved TODO")
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
    for name in ("feature-delivery", "testforge-evaluation"):
        skill_path = f".agents/skills/{name}/SKILL.md"
        metadata_path = f".agents/skills/{name}/agents/openai.yaml"
        frontmatter = parse_skill_frontmatter(skill_path)
        if frontmatter.get("name") != name:
            fail(skill_path, f"frontmatter name must be {name!r}")
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


def validate_manual_evaluations(source_contracts: dict[str, Any]) -> None:
    relative_path = "evals/manual-test-generation.jsonl"
    input_keys = source_contracts.get("manual_input_fields", set())
    criterion_fields = source_contracts.get("criterion_fields", set())
    java_enums = source_contracts.get("enums", {})
    records = load_jsonl(relative_path)
    if len(records) != 6:
        fail(relative_path, "must contain exactly six manual-generation cases")
    identifiers: set[str] = set()
    for index, record in enumerate(records, start=1):
        location = f"{relative_path}:{index}"
        expected_keys = {
            "id",
            "mode",
            "blocking",
            "fixtureVersion",
            "promptVersion",
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
        if record.get("fixtureVersion") != 1:
            fail(location, "fixtureVersion must be 1")
        if record.get("promptVersion") != "manual-test-v1":
            fail(location, "promptVersion must match GenerationService")

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
        required_keys = validate_string_list(
            expectations.get("requiredAcceptanceCriteria"),
            f"{location}:requiredAcceptanceCriteria",
        )
        if set(required_keys) != set(criterion_keys):
            fail(location, "requiredAcceptanceCriteria must include every supplied key")
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


def validate_automation_evaluations(source_contracts: dict[str, Any]) -> None:
    relative_path = "evals/automation-generation.jsonl"
    automation_components = source_contracts.get("automation_draft_components", set())
    records = load_jsonl(relative_path)
    if not 2 <= len(records) <= 3:
        fail(relative_path, "must contain two or three roadmap automation cases")
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
                fail(location, "requiredDraftSections must match AutomationDraft")
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
        "AGENTS.md": ("TestForge AI", "$feature-delivery", "$testforge-evaluation"),
        "README.md": ("docs/PRODUCT.md", "scripts/verify"),
        "CONTRIBUTING.md": ("ExecPlan", "scripts/verify"),
        "docs/PRODUCT.md": ("Stage 1", "AutomationDraftGenerator"),
        "docs/TESTING.md": ("no provider call", "automation-generation.jsonl"),
        "docs/PLANS.md": ("active/", "completed/"),
        "docs/product-specs/mvp-1-test-generation.md": ("manual-test-v1", "Stage 1"),
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


def main() -> int:
    validate_required_files()
    if ERRORS:
        print("TestForge harness validation failed:", file=sys.stderr)
        for error in ERRORS:
            print(f"- {error}", file=sys.stderr)
        return 1

    validate_agent_configuration()
    run_contract_parser_self_tests()
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
        "TestForge harness validation passed: 3 agents, 2 skills, "
        "6 blocking manual evals, and 3 non-blocking automation roadmap evals."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
