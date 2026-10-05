import {
  approveContract,
  approvalMatches,
  contractIssues,
  runSimulation,
  SimulationRunner,
} from './runner';
import { defaultEnvironment, fixtureSources, proposeFixture, sourceIssues } from './fixtures';
import {
  canonicalize,
  importStory,
  safeEvidenceJson,
  type Action,
  type FixtureId,
} from './contracts';

/** Creates a fresh supported contract for each isolated runner assertion. */
function plan(id: FixtureId = 'case') {
  return proposeFixture(fixtureSources[id], id, defaultEnvironment, {
    priority: 'High',
    owner: 'Synthetic Agent',
  });
}

describe('bounded Salesforce simulation', () => {
  it.each(['record', 'case'] as const)(
    'runs the %s fixture repeatably with complete AC evidence',
    (id) => {
      const contract = plan(id);
      const approval = approveContract(contract);
      const first = runSimulation(contract, approval);
      const second = runSimulation(contract, approval);
      expect(first).toMatchObject({
        lifecycle: 'COMPLETED',
        outcome: 'PASS',
        evidence: 'COMPLETE',
        records: 1,
      });
      expect(first.id).not.toBe(second.id);
      expect(second.steps.map((step) => step.after)).toEqual(first.steps.map((step) => step.after));
      expect(
        first.steps.filter((step) => step.outcome === 'PASS').flatMap((step) => step.criterionKeys),
      ).toEqual(expect.arrayContaining(['AC-1', 'AC-2']));
      expect(
        first.steps.every(
          (step) => step.startedAt && step.completedAt && step.action && step.criterionKeys.length,
        ),
      ).toBe(true);
    },
  );
  it('freezes a copy and invalidates every execution-affecting change', () => {
    const contract = plan();
    const approval = approveContract(contract);
    expect(Object.isFrozen(approval.contract.steps[0]!.action)).toBe(true);
    const mutations = [
      (copy: typeof contract) => {
        copy.source.title += ' revised';
      },
      (copy: typeof contract) => {
        copy.source.userStory += ' revised';
      },
      (copy: typeof contract) => {
        copy.source.acceptanceCriteria.reverse();
      },
      (copy: typeof contract) => {
        copy.steps[0]!.title += ' revised';
      },
      (copy: typeof contract) => {
        copy.environment.role = 'READ_ONLY';
      },
      (copy: typeof contract) => {
        copy.environment.id = 'SIM-UAT';
      },
      (copy: typeof contract) => {
        copy.environment.variables.recordName = 'Synthetic Revised';
      },
      (copy: typeof contract) => {
        copy.environment.secretReferences = ['OTHER_REFERENCE'];
      },
      (copy: typeof contract) => {
        copy.runnerVersion += 'changed';
      },
      (copy: typeof contract) => {
        copy.fixtureVersion += 'changed';
      },
      (copy: typeof contract) => {
        copy.clarification.owner = 'Synthetic Queue';
      },
      (copy: typeof contract) => {
        copy.steps[1]!.action = {
          type: 'ASSERT_FIELD',
          object: 'Case',
          field: 'Subject',
          expected: 'Synthetic Different',
        };
      },
    ];
    for (const mutate of mutations) {
      const copy = structuredClone(contract);
      mutate(copy);
      expect(approvalMatches(copy, approval)).toBe(false);
      expect(() => new SimulationRunner(copy, approval)).toThrow('stale');
    }
    expect(approvalMatches(contract, null)).toBe(false);
  });
  it('feeds clarification into the actual update and its assertion', () => {
    const contract = plan();
    const update = contract.steps.find((step) => step.action.type === 'UPDATE_RECORD');
    expect(update?.action).toMatchObject({
      values: { Priority: 'High', Owner: 'Synthetic Agent' },
    });
    const result = runSimulation(contract, approveContract(contract));
    expect(result.steps.at(-1)?.after).toMatchObject({
      Priority: 'High',
      Owner: 'Synthetic Agent',
    });
  });
  it('delivers duplicate write actions once and prevents reordered actions', () => {
    const contract = plan();
    const runner = new SimulationRunner(contract, approveContract(contract));
    expect(() => runner.deliver(contract.steps[1]!.id)).toThrow('order');
    const first = runner.deliver(contract.steps[0]!.id);
    expect(runner.deliver(contract.steps[0]!.id)).toBe(first);
    expect(runner.snapshot()).toMatchObject({ records: 1, steps: [first] });
    expect(runSimulation(contract, approveContract(contract), 'duplicate')).toMatchObject({
      records: 1,
      outcome: 'PASS',
    });
  });
  it('closes an interrupted write as indeterminate without blindly retrying', () => {
    const contract = plan();
    const runner = new SimulationRunner(contract, approveContract(contract));
    const first = runner.deliver(contract.steps[0]!.id, true);
    expect(first).toMatchObject({
      outcome: 'INDETERMINATE',
      after: null,
      failureCode: 'WRITE_ACKNOWLEDGEMENT_LOST',
    });
    expect(runner.deliver(contract.steps[0]!.id)).toBe(first);
    expect(() => runner.deliver(contract.steps[1]!.id)).toThrow('closed');
    expect(runner.snapshot()).toMatchObject({
      lifecycle: 'INTERRUPTED',
      outcome: 'INDETERMINATE',
      evidence: 'PARTIAL',
    });
    expect(runSimulation(contract, approveContract(contract), 'interrupt').steps).toHaveLength(1);
    expect(runSimulation(contract, approveContract(contract)).outcome).toBe('PASS');
  });
  it('enforces the approved read-only role before mutation', () => {
    const contract = plan();
    contract.environment.role = 'READ_ONLY';
    const run = runSimulation(contract, approveContract(contract));
    expect(run).toMatchObject({
      lifecycle: 'INTERRUPTED',
      outcome: 'NOT_EVALUATED',
      evidence: 'PARTIAL',
      records: 0,
    });
    expect(run.steps[0]!).toMatchObject({
      before: null,
      after: null,
      failureCode: 'PERMISSION_DENIED',
    });
  });
  it('separates business assertion failure from completed execution and complete evidence', () => {
    const contract = plan('record');
    const assertion = contract.steps.find(
      (step) => step.action.type === 'ASSERT_FIELD' && step.action.field === 'Industry',
    );
    if (assertion?.action.type === 'ASSERT_FIELD') assertion.action.expected = 'Finance';
    const run = runSimulation(contract, approveContract(contract));
    expect(run).toMatchObject({ lifecycle: 'COMPLETED', outcome: 'FAIL', evidence: 'COMPLETE' });
    expect(
      run.steps.some((step) => step.observation.includes('expected Finance; observed Technology')),
    ).toBe(true);
  });
  it('rejects unknown actions, fields, unmapped ACs and missing fixture obligations', () => {
    const contract = plan();
    const variants = [
      { ...contract, steps: contract.steps.slice(1) },
      { ...contract, steps: contract.steps.map((step) => ({ ...step, criterionKeys: ['AC-99'] })) },
      { ...contract, steps: contract.steps.map((step) => ({ ...step, criterionKeys: ['AC-1'] })) },
      {
        ...contract,
        steps: [
          {
            ...contract.steps[0]!,
            action: { type: 'EXECUTE_CODE', object: 'Case' } as unknown as Action,
          },
          ...contract.steps.slice(1),
        ],
      },
      {
        ...contract,
        steps: [
          {
            ...contract.steps[0]!,
            action: {
              type: 'CREATE_RECORD',
              object: 'Case',
              values: { Password: 'secret' },
            } as unknown as Action,
          },
          ...contract.steps.slice(1),
        ],
      },
    ];
    for (const variant of variants) {
      expect(contractIssues(variant).length).toBeGreaterThan(0);
      expect(() => approveContract(variant)).toThrow('blocking');
    }
  });
  it('rejects sensitive input and never includes a credential value in evidence', () => {
    const contract = plan();
    contract.source.userStory = 'token=synthetic-secret-canary';
    expect(() => approveContract(contract)).toThrow();
    expect(
      safeEvidenceJson({ error: 'Bearer synthetic-secret-canary', ok: 'Synthetic Queue' }),
    ).not.toContain('synthetic-secret-canary');
    const normal = plan();
    const evidence = safeEvidenceJson(runSimulation(normal, approveContract(normal)));
    expect(evidence).toContain('SALESFORCE_QA_CREDENTIAL');
    expect(evidence).not.toContain('synthetic-secret-canary');
  });
  it('validates blank source, custom criteria, unknown fixtures, and bounded local JSON', () => {
    expect(sourceIssues({ ...fixtureSources.case, title: '   ' }, 'case')).not.toEqual([]);
    expect(
      sourceIssues({ ...fixtureSources.case, acceptanceCriteria: ['   '] }, 'case'),
    ).not.toEqual([]);
    expect(
      sourceIssues({ ...fixtureSources.case, acceptanceCriteria: ['Launch a rocket'] }, 'case')[0],
    ).toContain('outside');
    expect(sourceIssues(fixtureSources.case, 'unknown' as FixtureId)).not.toEqual([]);
    expect(importStory(JSON.stringify(fixtureSources.case))).toEqual(fixtureSources.case);
    for (const value of [
      '{}',
      '{bad',
      JSON.stringify({ ...fixtureSources.case, acceptanceCriteria: [null] }),
      JSON.stringify({ ...fixtureSources.case, token: 'canary' }),
      'a'.repeat(24001),
    ])
      expect(() => importStory(value)).toThrow();
    expect(canonicalize({ b: 2, a: 1 })).toBe(canonicalize({ a: 1, b: 2 }));
  });
});
