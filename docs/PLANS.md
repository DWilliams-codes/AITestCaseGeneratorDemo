# Execution plans

Significant TestForge AI work uses a versioned ExecPlan so scope, contracts,
decisions, verification, and completion evidence survive an individual Codex
session.

Create `docs/exec-plans/active/TF-###-short-name.md` before implementation when
work changes product behavior, APIs, persistence, authentication, generation,
security controls, shared architecture, or spans multiple components. Small
documentation corrections and isolated test maintenance may use the pull request
description instead.

An ExecPlan must include:

- objective and explicit non-goals;
- acceptance criteria and source issue;
- current contracts inspected and assumptions resolved;
- exact files, interfaces, data migrations, and compatibility impact;
- ordered implementation and rollback approach;
- unit, integration, browser, security, and AI-evaluation coverage;
- risks, privacy considerations, and definition of done;
- actual commands, results, deviations, and residual risks at completion.

The architect prepares the plan without writing implementation files. One
builder owns all overlapping edits. The independent reviewer checks the plan,
diff, tests, security, and generated-output quality. Blocking findings return to
the same builder.

Do not mark a plan complete based on intended checks. Record observed evidence,
then move it from `active/` to `completed/` when the definition of done is met.
See [exec-plans/README.md](exec-plans/README.md) for naming and lifecycle rules.
