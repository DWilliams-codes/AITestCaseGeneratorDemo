import {
  canonicalize,
  fingerprint,
  freeze,
  FIXTURE_VERSION,
  RUNNER_VERSION,
  secretPattern,
  type Action,
  type Approval,
  type SimulationContract,
} from './contracts';
import { proposeFixture, sourceIssues } from './fixtures';

export type RunOutcome = 'PASS' | 'FAIL' | 'NOT_EVALUATED' | 'INDETERMINATE';
export interface StepEvidence {
  sequence: number;
  stepId: string;
  title: string;
  action: Action;
  criterionKeys: string[];
  startedAt: string;
  completedAt: string;
  outcome: RunOutcome;
  before: Record<string, string> | null;
  after: Record<string, string> | null;
  observation: string;
  failureCode: string | null;
}
export interface RunRecord {
  id: string;
  mode: 'SYNTHETIC_SIMULATION';
  approvalFingerprint: string;
  runnerVersion: string;
  environment: string;
  role: string;
  startedAt: string;
  completedAt: string | null;
  lifecycle: 'RUNNING' | 'COMPLETED' | 'INTERRUPTED';
  outcome: RunOutcome;
  approvedContract: SimulationContract;
  approvedAt: string;
  evidence: 'COMPLETE' | 'PARTIAL';
  steps: StepEvidence[];
  records: number;
}
const allowed: Record<string, string[]> = {
  Account: ['Name', 'Industry'],
  Case: ['Subject', 'Status', 'Priority', 'Owner'],
};
const enums: Record<string, string[]> = {
  Industry: ['Other', 'Technology', 'Finance'],
  Status: ['New', 'Working', 'Closed'],
  Priority: ['High', 'Medium', 'Low'],
  Owner: ['Synthetic Queue', 'Synthetic Agent'],
};

/** Rejects unsupported field names and keeps editable record values inside synthetic fixture data. */
function validField(object: string, field: string, value: unknown): boolean {
  if (!allowed[object]?.includes(field) || typeof value !== 'string' || secretPattern.test(value))
    return false;
  if (field === 'Name' || field === 'Subject')
    return /^Synthetic [A-Za-z0-9 .-]{1,60}$/.test(value);
  return enums[field]?.includes(value) ?? false;
}

/** Validates again at the runner boundary instead of trusting form widgets or a stale approval. */
export function contractIssues(contract: SimulationContract): string[] {
  const issues = sourceIssues(contract.source, contract.fixtureId);
  if (issues.length) return issues;
  if (contract.fixtureVersion !== FIXTURE_VERSION || contract.runnerVersion !== RUNNER_VERSION)
    issues.push('Fixture or runner version changed. Rebuild and review the proposal.');
  const environment = contract.environment;
  if (
    !['SIM-QA', 'SIM-UAT'].includes(environment.id) ||
    !['QA_EDITOR', 'READ_ONLY'].includes(environment.role) ||
    !/^Synthetic [A-Za-z0-9 .-]{1,60}$/.test(environment.variables.recordName) ||
    Object.keys(environment.variables).join() !== 'recordName' ||
    environment.secretReferences.length !== 1 ||
    environment.secretReferences[0] !== 'SALESFORCE_QA_CREDENTIAL'
  )
    issues.push(
      'Only maintained simulation environments, synthetic variables and the named unresolved reference are allowed.',
    );
  if (
    !['High', 'Medium'].includes(contract.clarification.priority) ||
    !['Synthetic Queue', 'Synthetic Agent'].includes(contract.clarification.owner)
  )
    issues.push('Clarification must use the supported synthetic choices.');
  if (secretPattern.test(canonicalize(contract)))
    issues.push('Remove sensitive input before review or evidence capture.');
  if (
    contract.steps.length < 1 ||
    contract.steps.length > 30 ||
    new Set(contract.steps.map((step) => step.id)).size !== contract.steps.length
  )
    issues.push('Steps must be bounded and have unique identities.');
  const maintained = proposeFixture(
    contract.source,
    contract.fixtureId,
    contract.environment,
    contract.clarification,
  );
  // Human edits may change expected values to demonstrate failed assertions, but may not remove
  // obligations, move mappings, replace operations or invent unsupported writes.
  /** Excludes only reviewer-editable labels and expectations when comparing maintained obligations. */
  const shape = (step: SimulationContract['steps'][number]) => ({
    ...step,
    title: undefined,
    action:
      step.action.type === 'ASSERT_FIELD' || step.action.type === 'ASSERT_RECORD_COUNT'
        ? { ...step.action, expected: undefined }
        : step.action,
  });
  if (canonicalize(contract.steps.map(shape)) !== canonicalize(maintained.steps.map(shape)))
    issues.push(
      'Maintain every supported fixture operation, field and criterion mapping. Unsupported action changes require a new fixture implementation.',
    );
  const coverage = new Set<string>();
  for (const step of contract.steps) {
    if (
      !/^step-[1-9][0-9]*$/.test(step.id) ||
      !step.title.trim() ||
      step.title.length > 200 ||
      !step.criterionKeys.length ||
      step.criterionKeys.some((key) => !['AC-1', 'AC-2'].includes(key))
    )
      issues.push('Every step needs a known criterion mapping and bounded title.');
    const action = step.action;
    if (action.object !== (contract.fixtureId === 'record' ? 'Account' : 'Case'))
      issues.push('Object is outside this fixture.');
    if (action.type === 'CREATE_RECORD' || action.type === 'UPDATE_RECORD') {
      const values = Object.entries(action.values);
      if (
        !values.length ||
        values.some(([field, value]) => !validField(action.object, field, value))
      )
        issues.push('Write contains unsupported fields or non-synthetic values.');
    } else if (action.type === 'ASSERT_FIELD') {
      if (!validField(action.object, action.field, action.expected))
        issues.push('Assertion contains an unsupported field or value.');
      step.criterionKeys.forEach((key) => coverage.add(key));
    } else if (action.type === 'ASSERT_RECORD_COUNT') {
      if (!Number.isInteger(action.expected) || action.expected < 0 || action.expected > 2)
        issues.push('Record-count assertion is outside fixture bounds.');
    } else issues.push('Unknown action: only the four typed simulation operations are accepted.');
  }
  for (const key of ['AC-1', 'AC-2'])
    if (!coverage.has(key)) issues.push(`${key} requires a mapped field assertion.`);
  return [...new Set(issues)];
}

/** Captures an immutable copy of everything reviewed, including environment, data and exact assertions. */
export function approveContract(contract: SimulationContract): Approval {
  if (contractIssues(contract).length)
    throw new Error('The proposal has blocking validation issues.');
  const copy = structuredClone(contract);
  return freeze({
    contract: copy,
    canonical: canonicalize(copy),
    fingerprint: fingerprint(copy),
    approvedAt: new Date().toISOString(),
  });
}

/** Checks the full canonical snapshot as well as its display fingerprint before any action. */
export function approvalMatches(contract: SimulationContract, approval: Approval | null): boolean {
  return (
    approval !== null &&
    approval.canonical === canonicalize(contract) &&
    approval.canonical === canonicalize(approval.contract) &&
    approval.fingerprint === fingerprint(contract) &&
    contractIssues(contract).length === 0
  );
}

/** Executes typed operations against an isolated in-memory record; it never calls a provider or Salesforce. */
export class SimulationRunner {
  private readonly contract: SimulationContract;
  private readonly record: RunRecord;
  private data: Record<string, string> | null = null;
  private readonly delivered = new Map<string, StepEvidence>();

  /** Copies and verifies the approved execution contract before creating a fresh synthetic run. */
  constructor(contract: SimulationContract, approval: Approval) {
    if (!approvalMatches(contract, approval))
      throw new Error('Approval is missing or stale. Review and approve the current proposal.');
    this.contract = structuredClone(approval.contract);
    this.record = {
      approvedContract: structuredClone(approval.contract),
      approvedAt: approval.approvedAt,
      id: crypto.randomUUID(),
      mode: 'SYNTHETIC_SIMULATION',
      approvalFingerprint: approval.fingerprint,
      runnerVersion: RUNNER_VERSION,
      environment: contract.environment.id,
      role: contract.environment.role,
      startedAt: new Date().toISOString(),
      completedAt: null,
      lifecycle: 'RUNNING',
      outcome: 'NOT_EVALUATED',
      evidence: 'PARTIAL',
      steps: [],
      records: 0,
    };
  }

  /** Delivers one ordered step once; a duplicate returns its previous evidence without replaying writes. */
  deliver(stepId: string, interruptAfterWrite = false): StepEvidence {
    const previous = this.delivered.get(stepId);
    if (previous) return previous;
    if (this.record.lifecycle !== 'RUNNING')
      throw new Error('This run is closed. Do not retry; begin a new isolated run.');
    const step = this.contract.steps[this.record.steps.length];
    if (!step || step.id !== stepId) throw new Error('Steps must be delivered in approved order.');
    const before = this.data ? { ...this.data } : null;
    let outcome: RunOutcome = 'NOT_EVALUATED';
    let observation = 'Synthetic action completed.';
    let failureCode: string | null = null;
    const action = step.action;
    const write = action.type === 'CREATE_RECORD' || action.type === 'UPDATE_RECORD';
    if (write && this.contract.environment.role !== 'QA_EDITOR') {
      failureCode = 'PERMISSION_DENIED';
      observation = 'The approved read-only role cannot write. No record changed.';
    } else if (action.type === 'CREATE_RECORD') {
      if (this.data) {
        failureCode = 'RECORD_ALREADY_EXISTS';
        observation = 'A second create was blocked.';
      } else {
        this.data = { ...action.values } as Record<string, string>;
        this.record.records = 1;
      }
    } else if (action.type === 'UPDATE_RECORD') {
      if (!this.data) {
        failureCode = 'RECORD_NOT_FOUND';
        observation = 'The update was blocked because no synthetic record exists.';
      } else this.data = { ...this.data, ...action.values } as Record<string, string>;
    } else if (action.type === 'ASSERT_FIELD') {
      outcome = this.data?.[action.field] === action.expected ? 'PASS' : 'FAIL';
      observation = `${action.field}: expected ${action.expected}; observed ${this.data?.[action.field] ?? '(missing)'}.`;
    } else {
      outcome = this.record.records === action.expected ? 'PASS' : 'FAIL';
      observation = `Record count: expected ${action.expected}; observed ${this.record.records}.`;
    }
    const interrupted = write && !failureCode && interruptAfterWrite;
    if (interrupted) {
      outcome = 'INDETERMINATE';
      failureCode = 'WRITE_ACKNOWLEDGEMENT_LOST';
      observation =
        'Write acknowledgement was interrupted. The result is unknown; no retry is permitted in this run.';
    }
    const evidence = freeze({
      sequence: this.record.steps.length + 1,
      stepId,
      title: step.title,
      action: structuredClone(action),
      criterionKeys: [...step.criterionKeys],
      startedAt: new Date().toISOString(),
      completedAt: new Date().toISOString(),
      outcome,
      before,
      after: interrupted ? null : this.data ? { ...this.data } : null,
      observation,
      failureCode,
    });
    this.record.steps.push(evidence);
    this.delivered.set(stepId, evidence);
    if (interrupted || failureCode) {
      this.record.lifecycle = 'INTERRUPTED';
      this.record.outcome = interrupted ? 'INDETERMINATE' : 'NOT_EVALUATED';
      this.record.completedAt = new Date().toISOString();
    } else if (this.record.steps.length === this.contract.steps.length) {
      this.record.lifecycle = 'COMPLETED';
      this.record.evidence = 'COMPLETE';
      this.record.outcome = this.record.steps.some((item) => item.outcome === 'FAIL')
        ? 'FAIL'
        : 'PASS';
      this.record.completedAt = new Date().toISOString();
    }
    return evidence;
  }

  /** Returns a detached immutable run record, excluding reference values and internal credentials. */
  snapshot(): RunRecord {
    return freeze(structuredClone(this.record));
  }
}

/** Runs a reviewed fixture with an optional synthetic failure or duplicate-delivery exercise. */
export function runSimulation(
  contract: SimulationContract,
  approval: Approval,
  mode: 'normal' | 'interrupt' | 'duplicate' = 'normal',
): RunRecord {
  const runner = new SimulationRunner(contract, approval);
  for (const step of contract.steps) {
    runner.deliver(step.id, mode === 'interrupt');
    if (mode === 'duplicate') runner.deliver(step.id);
    if (runner.snapshot().lifecycle !== 'RUNNING') break;
  }
  return runner.snapshot();
}
