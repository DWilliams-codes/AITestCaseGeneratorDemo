## Summary

- What changed:
- Why:

## Scope and plan

- Issue:
- Completed ExecPlan (materialized as the Builder's first write):
- Architect read-only plan handoff approved: yes / not applicable
- Post-build Architect conformance: pass / blockers / pending / not applicable
- Lead immediately assigned one Builder; Lead made no overlapping writes: yes / not applicable
- Builder re-read the active plan before implementation: yes / not applicable
- Independent Reviewer verdict: APPROVE / BLOCK / pending / not applicable
- Lead implementation-completion decision: complete / pending / not applicable
- Deviations from the plan:

## Verification

- [ ] `./scripts/verify.sh` or `.\scripts\verify.ps1`
- [ ] Relevant integration/browser checks
- [ ] `git diff --check`

Commands and observed results:

- Exact publication-candidate SHA (after local plan completion and separate authorization):
- GitHub/PR/external link to all seven exact-SHA CI results:
- Lead publication/merge decision: publish / merge / block / pending

## Evaluation impact

- Prompt/schema/validator/generated behavior changed: yes / no
- `$ai-generation-evals` impact recorded: yes / not applicable
- Manual fixture IDs exercised or updated:
- Score and hard failures, if an authorized semantic evaluation was run:
- Automation fixtures remain roadmap-only and non-blocking: yes / not applicable
- Live provider called by default verification or CI: no

## Security, privacy, and compatibility

- Data/authorization risks:
- API, persistence, or configuration compatibility:
- Synthetic-data and secret review completed: yes / no
- Residual risks and rollback:
- `$security-review` findings resolved: yes / pending / not applicable

## Documentation

- Documents updated:
- [ ] After `CONFORMS` and `APPROVE`, Lead decided implementation completion and the same Builder recorded final local evidence and moved the plan
- [ ] Any exact-SHA publication evidence is external to the candidate commit and is not required for local plan completion
