# TestForge AI interview walkthrough

## Main application: saved review workflow

Open **http://127.0.0.1:5173** on the existing desktop server. Use **Fill demo
credentials**, then **Sign in**. Credentials appear only when the backend has
verified its isolated public account. The banner explicitly says maintained
synthetic responses/no live AI. Use synthetic content only.

1. Open the saved canonical story **Create, route, and retain a Salesforce
   support case**. Show its title, user story, requirements and five ACs.
2. Explain that the proposed Medium/Synthetic Queue routing needs business
   confirmation. The initial generation exposes that question. Saving
   `Priority High; Owner Synthetic Agent` and regenerating changes concrete
   routing actions and expected observations; it does not merely hide a prompt.
3. Review the six manual cases: create, update, persistent reopen, blank Subject,
   and read-only create/update. Expand setup, synthetic data, steps and AC maps.
4. Edit a draft observation and save. Reload to show database persistence and
   revision history. Human approval remains an explicit separate action; the
   interview examples should remain unapproved unless deliberately reviewed.
5. Show generation provenance `external-demo-fixture / testforge-review-fixture`.
   These are maintained authoring fixtures, not a live AI evaluation. Salesforce
   tests are **NOT RUN** and no Salesforce org, Jira or ADO integration is connected.

Canonical import/source JSON is
`frontend/e2e/stub/fixtures/case-review-story.json`. It contains only synthetic
source requirements. The configured stub recognizes its exact title and ordered
five AC texts. Other synthetic browser examples do not receive its six-case
coverage claim. No credentials need to be entered in story content.

The desktop runtime is controlled by the Lead's local helper. Safe settings:
`SPRING_PROFILES_ACTIVE=local`, `SERVER_ADDRESS=127.0.0.1`,
`TESTFORGE_DEMO_SEED_ENABLED=true`, `TESTFORGE_DEMO_FIXTURE_MODE=true`,
`TEST_GENERATION_PROVIDER=openai`, `OPENAI_BASE_URL=http://127.0.0.1:8081/v1`,
`OPENAI_API_KEY=synthetic-e2e-only`. That key is deliberately public and cannot
authenticate a real provider. Start the maintained external stub with existing
Node from `frontend/e2e/stub/server.mjs`; never start duplicate listeners. The
backend needs the existing disposable PostgreSQL/JWT runtime configuration.
Do not use these public credentials for a normal provider configuration.

## Optional separate execution-controls simulation

Open **http://127.0.0.1:5173/simulation** on the existing local Vite server.
The simulation works without a backend, login or provider configuration. If the
server is stopped, use the existing dependencies: from `frontend`, run
`npm run dev -- --host 127.0.0.1`; check port 5173 first to avoid a duplicate.

## Five-minute demonstration

1. State the boundary: this is a synthetic Salesforce fixture, no connected org
   and no AI request. The saved workspace separately implements AI-assisted
   manual generation, human review and approved export.
2. Keep Case management selected. Show title, description and two explicit ACs.
   Change clarified priority/owner and select **Propose fixture steps**. Point
   out the concrete update data and expected priority/owner assertions.
3. Review steps and their AC mappings. Show reusable environment/role/variable
   settings and the unresolved credential-reference name. No credentials are
   entered per story. Check the review acknowledgement and approve.
4. Run and show lifecycle COMPLETED, assertion outcome PASS and evidence
   COMPLETE. Expand a step for before/after and expected/observed values. Expand
   the approved source contract; download JSON. Start a new isolated run and
   show repeatable outcomes with a distinct run ID.
5. Return to review and change an expected value; show Run disabled until new
   approval. A valid but wrong expectation demonstrates COMPLETED/FAIL/COMPLETE.
   Try the interrupt exercise to show INTERRUPTED/INDETERMINATE/PARTIAL and no
   blind retry. Select Read only and approve to demonstrate PERMISSION_DENIED
   before mutation. Account create/update also supports duplicate delivery with
   one record and one evidence row per action.

Import accepts local source JSON only. Use **Show fixture JSON**, then **Apply
pasted JSON** for the safe supported example. A custom AC is a manual draft and
cannot claim supported executable coverage. Azure DevOps and Jira are labeled
Unconnected. Refresh clears current proposals and the last-20-run history.

## Implemented, simulated and planned

Implemented existing workspace: authenticated owner-scoped stories, required
ACs, configured-provider manual generation, validation, saved clarifications
feeding the next generation, human edit/review/approval, traceability, approved
export and historical evidence. Model calls were not used for this preparation.

Simulated here: the two typed Salesforce-shaped workflows, immutable approval
contract, isolated repeatable execution, permissions, safe interruption,
duplicate handling and per-step downloadable evidence. This is deterministic
fixture behavior, not autonomous Salesforce coverage or real org validation.

Planned: authorized sandbox connector, server-enforced live execution, durable
multiuser run records, broader action/AC coverage, real Jira/Azure DevOps sync,
secret resolution and AI-generated executable proposals. No sandbox credentials
have been supplied. The current demo does not connect an org or incur AI charges.
