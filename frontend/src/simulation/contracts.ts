import { z } from 'zod';

export const RUNNER_VERSION = 'salesforce-fixture-runner-v1';
export const FIXTURE_VERSION = 'salesforce-fixtures-v1';
export const secretPattern =
  /(?:bearer\s+\S+|sk-[a-z0-9_-]{8,}|-----BEGIN.*PRIVATE KEY|(?:password|secret|token|api.?key)\s*[:=]\s*\S+|[a-z0-9._%+-]+@(?!example\.(?:com|test)\b)[a-z0-9.-]+\.[a-z]{2,})/i;
const text = z
  .string()
  .trim()
  .min(1)
  .max(4000)
  .refine(
    (value) => !secretPattern.test(value),
    'Use synthetic text only; remove credentials and personal data.',
  );
export const sourceSchema = z
  .object({
    title: text.max(200),
    userStory: text,
    acceptanceCriteria: z.array(text).min(1).max(20),
  })
  .strict();
export type StorySource = z.infer<typeof sourceSchema>;
export type FixtureId = 'record' | 'case';
export type ObjectName = 'Account' | 'Case';
export type FieldName = 'Name' | 'Industry' | 'Subject' | 'Status' | 'Priority' | 'Owner';
export type Action =
  | { type: 'CREATE_RECORD'; object: ObjectName; values: Partial<Record<FieldName, string>> }
  | { type: 'UPDATE_RECORD'; object: ObjectName; values: Partial<Record<FieldName, string>> }
  | { type: 'ASSERT_FIELD'; object: ObjectName; field: FieldName; expected: string }
  | { type: 'ASSERT_RECORD_COUNT'; object: ObjectName; expected: number };
export interface SimulationStep {
  id: string;
  title: string;
  criterionKeys: string[];
  action: Action;
}
export interface Environment {
  id: 'SIM-QA' | 'SIM-UAT';
  role: 'QA_EDITOR' | 'READ_ONLY';
  variables: { recordName: string };
  secretReferences: string[];
}
export interface SimulationContract {
  source: StorySource;
  fixtureId: FixtureId;
  fixtureVersion: string;
  runnerVersion: string;
  clarification: { priority: 'High' | 'Medium'; owner: 'Synthetic Queue' | 'Synthetic Agent' };
  environment: Environment;
  steps: SimulationStep[];
}
export interface Approval {
  contract: SimulationContract;
  canonical: string;
  fingerprint: string;
  approvedAt: string;
}

/** Orders object keys recursively so semantic comparisons do not depend on insertion order. */
export function canonicalize(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(canonicalize).join(',')}]`;
  if (value !== null && typeof value === 'object') {
    return `{${Object.entries(value)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([key, item]) => `${JSON.stringify(key)}:${canonicalize(item)}`)
      .join(',')}}`;
  }
  return JSON.stringify(value) ?? 'null';
}

/** Produces a compact display identifier; execution also compares the complete canonical contract. */
export function fingerprint(value: unknown): string {
  let hash = 2166136261;
  for (const character of canonicalize(value))
    hash = Math.imul(hash ^ character.charCodeAt(0), 16777619);
  return `SIM-${(hash >>> 0).toString(16).padStart(8, '0')}`;
}

/** Recursively freezes the copied approval and evidence objects against ordinary application edits. */
export function freeze<T>(value: T): T {
  if (value && typeof value === 'object') {
    Object.values(value).forEach(freeze);
    Object.freeze(value);
  }
  return value;
}

/** Parses only bounded source JSON; action programs, URLs and environment credentials cannot be imported. */
export function importStory(value: string): StorySource {
  if (new TextEncoder().encode(value).length > 24000)
    throw new Error('Import must be at most 24,000 bytes.');
  try {
    return sourceSchema.parse(JSON.parse(value));
  } catch {
    throw new Error(
      'Use JSON with title, userStory and 1–20 nonempty acceptanceCriteria strings. Use synthetic data only; extra fields are not accepted.',
    );
  }
}

/** Removes credential-like text from unexpected diagnostic data before downloadable evidence. */
export function safeEvidenceJson(value: unknown): string {
  return JSON.stringify(
    value,
    (_key, item: unknown) =>
      typeof item === 'string' && secretPattern.test(item) ? '[REDACTED]' : item,
    2,
  );
}
