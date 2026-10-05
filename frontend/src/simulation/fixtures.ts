import {
  FIXTURE_VERSION,
  RUNNER_VERSION,
  sourceSchema,
  type Environment,
  type FixtureId,
  type SimulationContract,
  type StorySource,
  type SimulationStep,
} from './contracts';

export const fixtureSources: Record<FixtureId, StorySource> = {
  record: {
    title: 'Create and update a Salesforce account',
    userStory:
      'As a sales operations specialist, I want to create a synthetic account and update its industry so the account record stays accurate.',
    acceptanceCriteria: [
      'Creating the synthetic account saves its name and creates exactly one Account record.',
      'Updating the Account industry to Technology preserves its name and record count.',
    ],
  },
  case: {
    title: 'Route and progress a Salesforce support case',
    userStory:
      'As a service agent, I want to create a synthetic case and move it to Working with the clarified priority and owner so the support team can act on it.',
    acceptanceCriteria: [
      'Creating the synthetic case saves its subject with New status and creates exactly one Case record.',
      'Updating the Case to Working saves the clarified priority and owner without creating another case.',
    ],
  },
};
export const defaultEnvironment: Environment = {
  id: 'SIM-QA',
  role: 'QA_EDITOR',
  variables: { recordName: 'Synthetic Acme' },
  secretReferences: ['SALESFORCE_QA_CREDENTIAL'],
};

/** Requires exact supported criterion semantics; arbitrary stories stay reviewable but cannot masquerade as executable coverage. */
export function sourceIssues(source: StorySource, fixtureId: FixtureId): string[] {
  if (!['record', 'case'].includes(fixtureId))
    return ['Unknown fixture. Load a supported workflow.'];
  if (!sourceSchema.safeParse(source).success)
    return [
      'Title, description and at least one nonempty criterion are required. Use bounded synthetic text only.',
    ];
  if (
    JSON.stringify(source.acceptanceCriteria.map((item) => item.trim())) !==
    JSON.stringify(fixtureSources[fixtureId].acceptanceCriteria)
  ) {
    return [
      "These criteria are outside this fixture's supported semantics. Keep this as a manual draft, or load a supported fixture before approving a simulation. No executable coverage is claimed for custom criteria.",
    ];
  }
  return [];
}

/** Builds explicit allowlisted operations from two maintained fixtures and the reviewed clarification. */
export function proposeFixture(
  source: StorySource,
  fixtureId: FixtureId,
  environment: Environment,
  clarification: SimulationContract['clarification'],
): SimulationContract {
  const object = fixtureId === 'record' ? 'Account' : 'Case';
  const steps: SimulationStep[] = [];
  /** Adds one independent action or assertion with an explicit criterion mapping. */
  const add = (title: string, criterion: string, action: SimulationStep['action']) =>
    steps.push({ id: `step-${steps.length + 1}`, title, criterionKeys: [criterion], action });
  if (fixtureId === 'record') {
    add('Create the synthetic account', 'AC-1', {
      type: 'CREATE_RECORD',
      object,
      values: { Name: environment.variables.recordName, Industry: 'Other' },
    });
    add('Verify the saved account name', 'AC-1', {
      type: 'ASSERT_FIELD',
      object,
      field: 'Name',
      expected: environment.variables.recordName,
    });
  } else {
    add('Create the synthetic support case', 'AC-1', {
      type: 'CREATE_RECORD',
      object,
      values: {
        Subject: environment.variables.recordName,
        Status: 'New',
        Priority: 'Medium',
        Owner: 'Synthetic Queue',
      },
    });
    add('Verify the saved case subject', 'AC-1', {
      type: 'ASSERT_FIELD',
      object,
      field: 'Subject',
      expected: environment.variables.recordName,
    });
    add('Verify initial case status', 'AC-1', {
      type: 'ASSERT_FIELD',
      object,
      field: 'Status',
      expected: 'New',
    });
  }
  add('Verify exactly one new record', 'AC-1', {
    type: 'ASSERT_RECORD_COUNT',
    object,
    expected: 1,
  });
  add(
    fixtureId === 'record'
      ? 'Update the account industry'
      : 'Route the case and move it to Working',
    'AC-2',
    {
      type: 'UPDATE_RECORD',
      object,
      values:
        fixtureId === 'record'
          ? { Industry: 'Technology' }
          : { Status: 'Working', Priority: clarification.priority, Owner: clarification.owner },
    },
  );
  if (fixtureId === 'record') {
    add('Verify the updated industry', 'AC-2', {
      type: 'ASSERT_FIELD',
      object,
      field: 'Industry',
      expected: 'Technology',
    });
    add('Verify the account name was preserved', 'AC-2', {
      type: 'ASSERT_FIELD',
      object,
      field: 'Name',
      expected: environment.variables.recordName,
    });
  } else {
    add('Verify case status', 'AC-2', {
      type: 'ASSERT_FIELD',
      object,
      field: 'Status',
      expected: 'Working',
    });
    add('Verify the clarified priority', 'AC-2', {
      type: 'ASSERT_FIELD',
      object,
      field: 'Priority',
      expected: clarification.priority,
    });
    add('Verify the clarified owner', 'AC-2', {
      type: 'ASSERT_FIELD',
      object,
      field: 'Owner',
      expected: clarification.owner,
    });
  }
  add('Verify update did not duplicate the record', 'AC-2', {
    type: 'ASSERT_RECORD_COUNT',
    object,
    expected: 1,
  });
  return {
    source: structuredClone(source),
    fixtureId,
    fixtureVersion: FIXTURE_VERSION,
    runnerVersion: RUNNER_VERSION,
    clarification: structuredClone(clarification),
    environment: structuredClone(environment),
    steps,
  };
}
