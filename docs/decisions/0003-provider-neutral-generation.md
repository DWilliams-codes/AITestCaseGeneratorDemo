# ADR 0003: Isolate and validate AI generation

- Status: Accepted
- Date: 2026-07-30

## Context

The product must be demonstrable without paid credentials, support a real provider, and never treat probabilistic output as trusted application data.

## Decision

Define an application-owned `TestGenerationProvider` contract. Supply a deterministic fake and a conditional OpenAI Responses API adapter. Version the prompt and strict schema. Minimize and delimit input, disable response storage, validate semantic invariants, allow one controlled retry, and persist only validated structured results plus bounded run metadata.

## Consequences

Provider changes do not alter workflow services or persistence. Strict validation may reject superficially plausible output, which is preferred to storing unsafe or untraceable cases. Model upgrades require schema compatibility tests and quality evaluation.
