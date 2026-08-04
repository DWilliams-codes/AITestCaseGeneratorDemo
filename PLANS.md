# Planning contract

Use an ExecPlan for significant TestForge work: product behavior, shared
architecture, API or persistence contracts, authentication/authorization,
security controls, generation behavior, or changes spanning components.

The Lead owns scope and implementation-completion decisions. A read-only
Architect establishes contracts, risks, acceptance criteria, validation, and
rollback and returns an approved plan handoff without writing repository files.
The Lead immediately assigns one Builder and never writes overlapping feature
files. The Builder's first repository write creates the active ExecPlan; the
Builder re-reads it before implementation and remains the sole writer through
review fixes. After local evidence, the Architect checks conformance and the
independent Reviewer returns `APPROVE` or `BLOCK`. Blockers return to that same
Builder.

Materialize active plans and record actual evidence according to
[TestForge execution plans](docs/PLANS.md) and the
[ExecPlan lifecycle](docs/exec-plans/README.md). Do not move a plan to
`completed/` on intended checks. After Architect `CONFORMS` and Reviewer
`APPROVE`, the Lead decides implementation completion; the same Builder then
records final local evidence and moves the file.

Local completion and publication are separate. Only after local plan completion,
and only with separate authorization, may a Lead/publisher stage, commit, and
push the final candidate. All seven CI jobs must pass that exact SHA before the
Lead decides publication or merge, with no repository write required. Store
exact-SHA results in GitHub/PR/external evidence or a later historical record,
not as a prerequisite inside the candidate commit. Never install dependencies,
call a live provider, execute generated automation, or mutate external state
merely to finish a plan.
