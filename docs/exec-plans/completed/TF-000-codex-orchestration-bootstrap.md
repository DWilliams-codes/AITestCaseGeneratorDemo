# TF-000: Codex orchestration bootstrap

## Status

Completed on 2026-08-03 on `codex/agent-orchestration-bootstrap` after final
independent reviewer approval.

## Objective

Make the TestForge AI repository the system of record for a bounded
architect-builder-reviewer delivery workflow, deterministic verification, and
versioned AI-output evaluations. This task changes only orchestration, harness,
documentation, evaluation fixtures, and CI configuration; it does not change
backend or frontend product behavior.

## Acceptance criteria

- Codex discovers one read-only architect, one workspace-write builder, and one
  read-only reviewer, with at most three concurrent agent threads.
- Repository guidance requires an ExecPlan, one writer, independent review,
  deterministic verification, and completion evidence.
- Reusable feature-delivery and TestForge evaluation skills are initialized
  with the standard skill creator, contain only the required UI metadata, and
  pass repository validation. The standard validator is also run when its
  existing runtime dependencies are available.
- Product, testing, planning, MVP, decision-index, and execution-plan docs match
  the implemented Stage 1 application and point to canonical existing docs.
- Six sanitized manual-generation evaluation cases encode the current prompt,
  schema, and generation contracts. Two or three automation cases are clearly
  marked as non-blocking roadmap fixtures and never invoke a live provider.
- The evaluation rubric totals 100 points, passes at 80, and defines hard
  failures.
- A deterministic harness validates agent TOML, skill metadata, JSONL fixtures,
  allowed enums, documentation links, and non-blocking automation status.
- PowerShell and Bash verification wrappers run tool preflights, backend checks,
  frontend checks, and the harness without installing dependencies.
- Existing CI jobs remain intact; a harness job is added and end-to-end checks
  depend on it.
- The pull request template records plan, verification, evaluation, risk, and
  documentation evidence.

## Source contracts inspected before evaluation authoring

- `backend/src/main/resources/prompts/test-generation-v1.txt`
- `backend/src/main/resources/prompts/test-generation-schema-v1.json`
- generation provider request/result types and result validator
- requirement and test-case DTO/domain enums
- Stage 1 integration and generation-validation tests

## Planned files

Create:

- `AGENTS.md`
- `.codex/config.toml`
- `.codex/agents/architect.toml`
- `.codex/agents/builder.toml`
- `.codex/agents/reviewer.toml`
- `.agents/skills/feature-delivery/SKILL.md`
- `.agents/skills/feature-delivery/agents/openai.yaml`
- `.agents/skills/testforge-evaluation/SKILL.md`
- `.agents/skills/testforge-evaluation/agents/openai.yaml`
- `docs/PRODUCT.md`
- `docs/TESTING.md`
- `docs/PLANS.md`
- `docs/product-specs/mvp-1-test-generation.md`
- `docs/decisions/README.md`
- `docs/exec-plans/README.md`
- `docs/exec-plans/active/.gitkeep`
- `evals/manual-test-generation.jsonl`
- `evals/automation-generation.jsonl`
- `evals/RUBRIC.md`
- `scripts/validate-harness.py`
- `scripts/verify.ps1`
- `scripts/verify.sh`
- `.github/pull_request_template.md`

Minimally update:

- `README.md`
- `CONTRIBUTING.md`
- `.github/workflows/ci.yml`

This file remains under `docs/exec-plans/active/` until independent review is
complete. Actual validation results and deviations are recorded below.

## Implementation sequence

1. Inspect the real prompt, JSON schema, DTOs, enums, tests, package scripts,
   Maven lifecycle, CI jobs, and canonical documentation.
2. Initialize both skills with `init_skill.py`, then customize them using
   patches and validate them with `quick_validate.py`.
3. Add agents, workflow guidance, product/testing/planning documentation, and
   evaluation fixtures grounded in the inspected contracts.
4. Add the deterministic harness and cross-platform verification wrappers.
5. Add PR evidence prompts and the CI harness gate while preserving all jobs.
6. Run the harness, skill validation, frontend checks, backend checks where
   available, wrapper smoke tests, and `git diff --check`.
7. Record actual results, deviations, and residual risks, then move this plan
   to `completed/`.

## Constraints and risks

- Do not call OpenAI or any other live model provider.
- Do not install or upgrade dependencies.
- Preserve existing production Java and React behavior and public contracts.
- Treat automation generation as roadmap-only and non-blocking until the
  product implements that output path.
- Verification wrappers must fail clearly when required tools or installed
  dependencies are absent; they must not mutate the environment to repair it.

## Definition of done

All acceptance criteria are represented in versioned files, deterministic
validation passes in the available environment, the diff contains no production
behavior change, CI retains its existing checks, and this plan is completed with
the evidence actually observed.

## Actual results

- Created the three agent profiles with validated roles: architect and reviewer
  are read-only, builder is workspace-write, and concurrency is capped at three.
- Initialized both skills with the standard `init_skill.py`, supplied only
  `display_name`, `short_description`, and `default_prompt`, then customized
  their `SKILL.md` files with patches.
- Added the product/workflow documentation, six blocking sanitized manual
  fixtures, three non-blocking Stage 2 roadmap fixtures, and the 100-point rubric
  with an 80-point/no-hard-failure gate.
- Added a standard-library-only harness that validates the live prompt/schema
  contract, agent TOML, skill frontmatter/UI metadata, JSONL shapes and the enum
  values represented by fixture expectations, rubric invariants, documentation
  terms and local links, roadmap status, provider request/result record shapes,
  and all seven source-derived `AutomationDraft` record components.
- Added PowerShell and Bash wrappers with non-mutating preflights. Added a CI
  harness job that runs the Bash harness-only path, an event-aware commit-range
  whitespace check, and a non-executing PowerShell parser check; all prior jobs
  remain, and end-to-end now depends on the harness.
- Added the pull request evidence template and minimal workflow links in the
  existing README and contributing guide.
- No backend Java, frontend React/TypeScript, database migration, prompt, schema,
  or other production behavior file was changed. No project or global
  dependency was changed, and no provider was called.

## Validation evidence

- `python scripts/validate-harness.py` — passed: 3 agents, 2 skills, 6 blocking
  manual evals, and 3 non-blocking automation roadmap evals. This includes
  deterministic enum/record parser self-tests, Java/schema enum comparisons,
  provider-visible input checks, request/result record/schema comparisons,
  automation-record drift checks, and prompt-version drift checks.
- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify.ps1 -HarnessOnly`
  — passed.
- Python compile check for `scripts/validate-harness.py` — passed without writing
  bytecode.
- PowerShell parser check for `scripts/verify.ps1` — passed with no parse errors.
- Official `quick_validate.py` checks for both `feature-delivery` and
  `testforge-evaluation` — passed with `Skill is valid!` using the disposable
  `testforge-skill-validation-019fc9ea` temporary virtual environment and
  PyYAML 6.0.3.
- CI YAML parse — passed with PyYAML 6.0.3 and confirmed the `jobs.harness`
  mapping is present.
- Frontend `npm run format:check`, `lint`, `typecheck`, `test:coverage`, and
  `build` — passed. Vitest reported 4 files and 15 tests passed; coverage was
  89.44% statements, 69.75% branches, 83.45% functions, and 89.81% lines.
- `git diff --cached --check` — passed against the explicitly staged TF-000
  patch after the plan evidence update; Git reported only informational checkout
  line-ending warnings while staging on Windows.
- Full `scripts/verify.ps1` — the harness passed, then the required-tool
  preflight stopped as designed because Maven is not installed on PATH. The
  only Java on PATH is version 11, while the project requires Java 21.

## Deviations and unavailable checks

- An explicitly authorized disposable virtual environment was created at
  `testforge-skill-validation-019fc9ea`, and PyYAML 6.0.3 was installed only to
  run the official `quick_validate.py` script and parse CI YAML. It is outside
  the repository and changes no project or global dependency. Repository
  verification itself uses only the Python standard library and has no PyYAML
  dependency.
- Backend Maven verification could not run because Maven and Java 21 are absent
  from the available environment. Existing backend CI remains unchanged and
  continues to run `mvn verify` on Java 21.
- Bash, Git Bash, and WSL are unavailable locally, so the Bash wrapper could not
  be executed here. Its harness-only path is now exercised by the Ubuntu CI job.
- No browser or live-generation suite was run: production UI behavior did not
  change, the browser stack was not started, and live provider calls are
  prohibited for TF-000.

## Architect review remediation

The architect reviewed the first implementation pass and returned three
blocking harness findings. TF-000 was moved from completed back to active before
remediation.

- Corrected every roadmap automation fixture to require the `name` component in
  addition to setup, test, cleanup, parameters, unresolved placeholders, and
  suitability score.
- Added a source-contract drift check that reads the seven record components
  from `AutomationDraft.java`; the fixture validator now compares
  `requiredDraftSections` to that complete expected set.
- Hardened the CI harness job with `git diff --check` and PowerShell AST parsing
  without executing `verify.ps1`, while preserving the Bash harness-only run.
- Re-ran the harness, PowerShell harness-only path and parser, both official
  skill validators, and `git diff --check`; all passed. Frontend checks were not
  rerun because remediation changed only harness, evaluation, CI, and plan files;
  the previously recorded frontend pass remains applicable.

The plan intentionally remains active until the independent reviewer completes
the next workflow gate.

## Reviewer findings remediation

The independent reviewer returned three blocking findings. The plan remained
active and the same builder applied all fixes.

- Replaced the clean-checkout no-op whitespace command in CI with event-aware
  ranges: pull requests check base SHA through head SHA, pushes check before SHA
  through current SHA, and all-zero `before` values compare the current commit
  with Git's empty tree. The harness checkout now uses `fetch-depth: 0`.
- Replaced hard-coded Python enum authorities with parsers for
  `TestCaseCategory`, `TestPriority`, `DataSensitivity`, `CoverageIntent`, and
  `AmbiguityCategory`. Each parsed Java enum must equal its corresponding JSON
  schema enum; fixture expectation values are checked against those parsed
  contracts.
- Added parsing for `TestGenerationRequest`, `CriterionInput`,
  `TestGenerationResult`, its relevant nested records, and the minimized fields
  serialized by `OpenAiTestGenerationProvider`. Structured-output record
  components must equal JSON schema property sets, and schema `required` sets
  must equal their property sets.
- Retained source parsing for `AutomationDraft` and added deterministic parser
  self-tests proving that removed or renamed enum constants and record components
  differ from their expected contracts.
- Staged the complete TF-000 patch by explicit path, validated the cached patch,
  updated this evidence, restaged this plan, and validated the cached patch
  again. No commit or push was performed.
- Corrected dependency evidence to distinguish the authorized disposable PyYAML
  environment from unchanged project/global dependencies.

## Final reviewer approval

The independent reviewer re-reviewed the complete staged TF-000 patch after all
three blocking findings were remediated and approved it with no blocking
findings. The same builder then recorded this approval and moved the plan from
`active/` to `completed/` while preserving `active/.gitkeep`.

After the lifecycle move, the complete staged patch passed
`git diff --cached --check` and `python scripts/validate-harness.py`. No commit or
push was performed. All TF-000 workflow gates are satisfied.

## Residual risks

- The first remote CI run must confirm Bash-wrapper execution, GitHub Actions
  YAML interpretation, and unchanged backend verification in its provisioned
  Linux/Java 21/Maven environment.
- The evaluation harness validates fixture contracts and invariants, not model
  quality. Semantic scoring still requires an explicitly authorized, sanitized
  candidate output and human review; automation fixtures remain non-blocking.
