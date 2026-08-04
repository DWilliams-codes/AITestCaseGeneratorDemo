# Automation draft and Copado architecture

## Status

This is a future design reference. TF-001 and the current Stage 1 product do
not generate, export, or execute Copado Robotic Testing assets.

## Boundary

Automation begins only from an approved manual-test snapshot. A provider-
neutral `AutomationDraftGenerator` produces a reviewed intermediate
representation (IR); vendor adapters serialize that IR. The model never emits
an executable deployment directly.

```mermaid
flowchart LR
  Approved["Approved case snapshot"] --> Suitability["Suitability and gaps"]
  Suitability --> Draft["Neutral automation IR"]
  Draft --> Validate["Schema, policy, and selector validation"]
  Validate --> Review["Human automation review"]
  Review --> Adapter["Copado adapter"]
  Adapter --> Package["Non-executing draft package"]
  Package --> External["Explicit external import/deploy"]
```

## Neutral IR

The IR should carry source snapshot IDs, target application profile, ordered
setup/action/assertion/cleanup steps, typed parameters, synthetic data
references, selector requirements, reusable-action references, expected
results, secret placeholders, suitability score, unsupported gaps, and
adapter-version metadata. It must not contain active credentials, environment
tokens, hidden reasoning, or unreviewed destructive actions.

Selectors are typed placeholders until resolved against an approved application
map. Secrets are references to an external secret manager, never values. Every
action declares retry/idempotency behavior and cleanup expectations. Unsupported
manual steps remain explicit gaps rather than fabricated automation.

## Source-resource paste and upload model

Future paste/upload creates a `SourceResource` catalog row before any AI or
automation use. The row records workspace/project, origin (`PASTE`, `UPLOAD`, or
approved connector), original safe filename, declared and detected media type,
byte length, SHA-256 digest, actor/time, scan result, parser/version, extraction
status, and immutable extracted-text snapshot. Duplicate hashes may reuse bytes
but never reuse authorization or provenance across workspaces.

The accepted initial formats should be an explicit allowlist of bounded UTF-8
text, Markdown, JSON, CSV, and separately reviewed Copado `.resource` data.
Archives, encrypted inputs, polyglots, macros, binaries, symlinks, path
traversals, remote includes, and type/extension mismatches are rejected or
quarantined. Uploaded bytes remain outside executable/class paths and are never
loaded through reflection, package managers, template engines, or interpreters.

A `.resource` file is parsed as data by a dedicated, version-pinned grammar in
an isolated worker with CPU/memory/time/output limits and no network, secrets,
or application database credential. Parsing extracts only allowlisted keywords,
arguments, variables, and documentation into a neutral catalog. It never runs
resource keywords, imports referenced libraries, resolves dynamic expressions,
or follows filesystem/network paths.

Imported Python (`.py`, `.pyc`, wheels), JavaScript, shell, Java archives,
native libraries, and any unknown executable/library declaration are rejected
from the normal resource path. If a later product requirement needs forensic
inspection, bytes stay quarantined and an isolated offline scanner may produce
metadata only; they are never imported into the application or Copado renderer.

## Copado adapter

The future adapter maps supported neutral actions to a pinned Copado schema and
version, escapes user-controlled content, preserves source traceability, and
emits a draft package only. Organization-specific reusable actions, selectors,
data profiles, environments, naming rules, and approval policy are adapter
inputs, not model inventions.

Import, deployment, scheduling, and execution are separate privileged actions
requiring explicit authorization, audit evidence, environment allowlists, and
human confirmation. Imported results link back to the exact draft and source
snapshots and classify infrastructure failure separately from product failure.

The adapter consumes only a validated neutral-draft snapshot and an approved,
versioned organization catalog. Rendering is deterministic and side-effect
free: no network, filesystem discovery, dynamic templates, plugin loading, or
code execution. It emits to an in-memory bounded model, escapes every
user-controlled field, applies stable ordering, and computes a digest over the
final package and manifest.

An independent Copado validator reparses the rendered package and proves schema
version, manifest/digest, source snapshot links, action allowlist, selector and
variable references, secret absence, bounds, cleanup, and semantic equivalence
to the neutral draft. Export sets safe content type/disposition and filename,
prevents spreadsheet/markup/path injection, and labels the artifact
`NON_EXECUTING_DRAFT`. Export never imports, deploys, schedules, or invokes it.

## Validation and evaluation

Validate schema, allowed action types, bounded loops/retries, selector
completeness, secret absence, assertion quality, cleanup, and traceability.
Test parser bombs, malformed `.resource` syntax, nested/dynamic imports, Python
and unknown libraries, digest mismatch, renderer injection, path traversal,
oversized expansion, cross-workspace resource IDs, and neutral/rendered semantic
drift. Parser/renderer tests use inert synthetic fixtures and no Copado tenant.
Automation fixtures remain non-blocking roadmap data until the IR and scoring
rubric are approved. Copado-specific benchmark promotion requires sanctioned
synthetic application metadata and must never use production selectors.
