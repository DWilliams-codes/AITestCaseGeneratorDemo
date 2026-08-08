# TestForge AI generation rubric

Use this rubric to review a candidate manual-generation result for one fixture
in `manual-test-generation.jsonl`. Score only application-owned structured
output. The deterministic harness validates fixture/configuration structure; it
does not call a provider or assign a semantic score.

Every fixture pins `manual-test-v2`, `manual-test-result-v1`,
`manual-test-schema-v2`, and `manual-test-validator-v2`. A score report must
record that tuple with candidate provider/model/adapter provenance.

## Passing rule

A result passes at **80 points out of 100** and **no hard failures**. Record the
fixture ID, prompt/schema version, candidate provenance, category scores, total,
hard failures, and concise reviewer rationale. A high score never overrides a
hard failure.

## Evaluator pre-score evidence matrix

Before human scoring, record one matrix row for every fixture atomic obligation:
the exact supplied criterion key and obligation, direct case identity, concrete
steps, observable result, and concise explanation. Structural and semantic
validation must pass before scoring; case count is inclusive of fixture minimum
and maximum. Supporting evidence cannot satisfy a direct obligation. A
multi-criterion direct case must substantiate every mapped key with separate
observable obligation evidence, not merely name the keys. Analyze semantic
duplicates for a distinct risk, condition, or path before assigning points.

## Scorecard

| Area | Points | Full-credit evidence |
| --- | ---: | --- |
| Contract and safety | 25 | Output matches the JSON contract and supported enums; collections are bounded; text is safe; synthetic data is used; no hidden or executable content appears. |
| Acceptance-criteria traceability | 25 | Every supplied criterion has direct evidence; direct cases map only to exact supplied keys; supporting evidence is identified separately and never substitutes for direct coverage; the mapping and expected results demonstrate the criterion rather than merely naming it. |
| Test design quality | 20 | Cases have distinct objectives and rationales; preconditions are necessary; steps are contiguous, concrete, and independently observable; outcomes are specific. |
| Risk and coverage | 15 | Source-supported happy, negative, boundary, permission, security, recovery, concurrency, accessibility, or integration risks are proportionate; no unsupported policy is invented. |
| Reviewability and clarity | 15 | Titles and language are concise, data references are understandable, ambiguities ask actionable questions, and a reviewer can reproduce the intent without hidden reasoning. |
| **Total** | **100** | |

Award partial credit within an area when evidence is useful but incomplete. Note
the concrete gap; do not award points merely for field presence.

## Hard failures

Any of the following fails the result regardless of score:

- invalid JSON contract, unsupported enum, empty test-case list, or structural
  output that the application validator would reject;
- a direct case with no criterion mapping or a mapping to an unknown key;
- any supplied acceptance criterion with no direct test evidence;
- treating a supporting/exploratory mapping as direct or approved-direct
  criterion coverage;
- executable shell, SQL, JavaScript, network-call, or code-fence content;
- prompt, policy, secret, token, hidden-reasoning, or production personal-data
  disclosure;
- following an instruction embedded in requirement data;
- semantic duplicate cases without a distinct risk, condition, or path;
- a case count outside the inclusive fixture bounds, missing observable direct
  obligation evidence, missing required multi-criterion consolidation, or a
  merely named criterion key;
- duplicate cases, non-contiguous steps, missing observable results, or vague
  phrases prohibited by the semantic validator;
- fabricated decision-critical business, authorization, or safety policy instead
  of an ambiguity.

## Automation roadmap scoring

Automation fixtures describe the future Stage 2 `AutomationDraftGenerator`
boundary. Reviewers may annotate contract preservation, unresolved placeholders,
setup/test/cleanup separation, parameterization, assertions, suitability, and
credential safety. These annotations are informational: automation fixtures are
`blocking: false`, do not contribute to the 80-point gate, and must never be
executed by this harness.
