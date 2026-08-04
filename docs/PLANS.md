# TestForge execution plans

Significant TestForge AI work uses a versioned ExecPlan so scope, contracts,
decisions, verification, and completion evidence survive an individual Codex
session.

Use `docs/exec-plans/active/TF-###-short-name.md` before implementation when work
changes product behavior, APIs, persistence, authentication, generation,
security controls, shared architecture, or spans multiple components. The
read-only Architect first returns an approved plan handoff; the Lead immediately
assigns one Builder, whose first repository write materializes that handoff and
who re-reads it before implementation. Small documentation corrections and
isolated test maintenance may use the pull request description instead.

An ExecPlan must include:

- objective and explicit non-goals;
- acceptance criteria and source issue;
- current contracts inspected and assumptions resolved;
- exact files, interfaces, data migrations, and compatibility impact;
- ordered implementation and rollback approach;
- unit, integration, browser, security, and AI-evaluation coverage;
- risks, privacy considerations, and definition of done;
- actual commands, results, deviations, and residual risks at completion.

The Lead approves scope, immediately assigns the Builder after the Architect
handoff, and never writes overlapping feature files. The Architect remains
read-only. One Builder materializes the plan first, re-reads it, and owns every
overlapping implementation and remediation edit. After local evidence, the
Architect returns `CONFORMS` or `BLOCK`; only after `CONFORMS` does the
independent Reviewer return `APPROVE` or `BLOCK`. Blocking findings return to
the same Builder.

Do not mark a plan complete based on intended checks. After Architect `CONFORMS`
and Reviewer `APPROVE`, the Lead decides implementation completion; the same
Builder records final local evidence and moves the plan from `active/` to
`completed/`.
See the root [planning contract](../PLANS.md) and
[exec-plans/README.md](exec-plans/README.md) for naming and lifecycle rules.

Local implementation completion and publication readiness are separate. Only
after the plan is completed, and only if separately authorized, may a
Lead/publisher stage, commit, and push the final candidate. All seven CI jobs
must pass that exact SHA before the Lead decides publication or merge; no
repository write is required for that decision. Keep exact-SHA results in
GitHub/PR/external evidence or a later historical record, not as a
self-referential requirement inside the candidate commit.
