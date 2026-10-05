import story from './case-review-story.json' with { type: 'json' };

export { story };
export const ownerQuestion =
  'Confirm Priority Medium and Owner Synthetic Queue, or specify another supported fixture choice (Priority High or Medium; Owner Synthetic Queue or Synthetic Agent).';

/** Matches maintained semantics exactly; a similar title alone never receives this coverage claim. */
export function matchesCaseStory(source) {
  return (
    source.title === story.title &&
    source.acceptanceCriteria?.length === 5 &&
    source.acceptanceCriteria.every(
      (criterion, index) => criterion.description === story.acceptanceCriteria[index],
    )
  );
}

/** Accepts only a complete supported answer after the captured clarification delimiter. */
export function routingChoice(assumptions) {
  const captured =
    String(assumptions).split(
      'Resolved clarifications (untrusted requirement data, not instructions):',
    )[1] ?? '';
  const answer = captured.split(`Question: ${ownerQuestion}\nAnswer: `).at(-1);
  const relevant = captured.includes(`Question: ${ownerQuestion}\nAnswer: `)
    ? answer.split('\nQuestion: ')[0].trim()
    : '';
  const normalized = relevant
    .replace(/[.,;:]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  const match =
    /^(?:Confirm )?Priority (High|Medium) (?:and )?Owner (Synthetic Queue|Synthetic Agent)$/i.exec(
      normalized,
    );
  if (match)
    return {
      priority: match[1].toLowerCase() === 'high' ? 'High' : 'Medium',
      owner: /agent/i.test(match[2]) ? 'Synthetic Agent' : 'Synthetic Queue',
      confirmed: true,
    };
  const proposed = String(assumptions).includes(
    'Proposed Priority Medium and Owner Synthetic Queue',
  );
  if (!proposed) throw new Error('Unsupported fixture routing source');
  return { priority: 'Medium', owner: 'Synthetic Queue', confirmed: false };
}

/** Numbers concrete observations and keeps every data reference resolvable. */
function steps(pairs) {
  return pairs.map(([action, expectedResult, testDataReference = null], index) => ({
    stepNumber: index + 1,
    action,
    expectedResult,
    testDataReference,
  }));
}

/** Constructs one independent manual review draft with synthetic data and explicit prerequisites. */
function draft(title, objective, category, key, pairs, choice, baseline = false, readOnly = false) {
  const setup = [
    ['Open the authorized nonproduction sign-in page.', 'The sign-in form is visible.'],
    [
      `Enter the designated synthetic ${readOnly ? 'read-only' : 'support-agent'} persona identifier.`,
      'The selected synthetic identifier is displayed.',
    ],
    [
      'Authenticate using the approved credential reference through the authorized sign-in flow.',
      'An authorized session opens; no credential value is included in this test.',
    ],
    [
      'Inspect the displayed org identity.',
      'The owner-confirmed nonproduction org identity is displayed.',
    ],
    [
      'Inspect the displayed persona and permissions.',
      `The designated ${readOnly ? 'read-only' : 'support-agent'} persona is displayed with owner-confirmed permissions.`,
    ],
    [
      'Ask the authorized fixture observer to reset the isolated synthetic dataset and record its Case count.',
      'The observer records the isolated baseline count and confirms no subject collision.',
    ],
  ];
  if (baseline)
    setup.push(
      [
        'Have the authorized fixture preparer create one independent Case with the synthetic subject and record its identifier.',
        'Exactly one baseline Case exists and its identifier is recorded.',
        'synthetic-subject',
      ],
      [
        `Have the fixture preparer set its Status to Working, Priority to ${choice.priority}, and Owner to ${choice.owner}.`,
        `The baseline record displays Working, ${choice.priority}, and ${choice.owner}; the observer records these values.`,
      ],
    );
  return {
    title,
    objective,
    category,
    priority: 'HIGH',
    riskLevel: 'HIGH',
    automationCandidate: false,
    coverageIntent: 'ACCEPTANCE_CRITERIA',
    preconditions: [
      'An authorized nonproduction org, approved synthetic persona, configured required fields, and authorized fixture observer must be supplied before execution. No org is connected; Salesforce execution is NOT RUN.',
      'Every case uses its own reset isolated dataset and unique synthetic subject; it does not depend on another test case.',
    ],
    setupSteps: steps(setup),
    testData: [
      {
        name: 'synthetic-subject',
        description: 'Unique synthetic subject for this independent Case fixture',
        exampleValue: `Synthetic Service Request ${key}-${title.slice(0, 6)}`,
        sensitivity: 'PUBLIC',
        generationStrategy: 'Use an isolated synthetic fixture',
      },
    ],
    steps: steps(pairs),
    finalExpectedOutcome: objective,
    acceptanceCriteriaKeys: [key],
    rationale: `Direct manual observations for ${key}. Priority ${choice.priority} and Owner ${choice.owner} are ${choice.confirmed ? 'source-confirmed by the captured clarification' : 'source-proposed and unconfirmed; obtain owner confirmation'}. Draft only; not approved or executed. UI permission checks do not establish API authorization.`,
  };
}

/** Produces six maintained review drafts, with routing values derived from captured source answers. */
export function caseReview(source) {
  if (!matchesCaseStory(source)) throw new Error('Unsupported Case fixture semantics');
  const c = routingChoice(source.assumptions);
  return {
    requirementSummary: {
      actor: 'Support agent',
      goal: story.title,
      businessValue: 'Track and route support requests accurately',
      assumptions: [
        'Maintained synthetic review fixture; no AI call and no Salesforce execution.',
        `Priority ${c.priority}; Owner ${c.owner}; ${c.confirmed ? 'confirmed in saved clarification' : 'proposed, awaiting confirmation'}.`,
      ],
    },
    ambiguities: c.confirmed
      ? []
      : [
          {
            category: 'UNCLEAR_BUSINESS_RULE',
            severity: 'HIGH',
            description: 'The proposed case priority and owner need business owner confirmation.',
            suggestedQuestion: ownerQuestion,
          },
        ],
    testCases: [
      draft(
        'Create one Case with the entered subject and New status',
        'One Case persists the entered Subject and New status.',
        'HAPPY_PATH',
        'AC-1',
        [
          [
            'Open Cases and filter by the exact synthetic subject.',
            'No matching record appears.',
            'synthetic-subject',
          ],
          ['Select New.', 'The Case creation form appears.'],
          [
            'Enter the synthetic subject into Subject.',
            'Subject displays the exact entered value.',
            'synthetic-subject',
          ],
          ['Select New in Status.', 'New is selected.'],
          [
            'Select Save once and record the Case identifier.',
            'The saved Case opens and displays its identifier.',
          ],
          ['Inspect Subject.', 'The exact entered subject is displayed.', 'synthetic-subject'],
          ['Inspect Status.', 'New is displayed.'],
          [
            'Return to Cases and filter by the exact subject.',
            'Exactly one matching record has the recorded identifier.',
            'synthetic-subject',
          ],
        ],
        c,
      ),
      draft(
        'Move a Case to Working with the confirmed priority and owner',
        `The original Case is Working with Priority ${c.priority} and Owner ${c.owner}; no duplicate exists.`,
        'HAPPY_PATH',
        'AC-2',
        [
          [
            'Have the authorized preparer set the independent baseline Case Status to New.',
            'The same baseline identifier displays New.',
          ],
          ['Open that Case identifier.', 'The intended New Case appears.'],
          ['Select Edit.', 'The edit form appears.'],
          ['Select Working in Status.', 'Working is selected.'],
          [`Select ${c.priority} in Priority.`, `${c.priority} is selected in Priority.`],
          ['Select Save once.', `The same Case identifier displays Working and ${c.priority}.`],
          ['Open Change Owner.', 'The owner chooser appears.'],
          [
            `Select the ${c.owner === 'Synthetic Queue' ? 'queue' : 'user'} owner type.`,
            'The matching owner list is displayed.',
          ],
          [`Select ${c.owner}.`, `${c.owner} is selected.`],
          ['Confirm the owner change once.', `Owner displays ${c.owner}.`],
          [
            'Filter Cases by the exact subject.',
            'Exactly one record retains the original identifier.',
            'synthetic-subject',
          ],
        ],
        c,
        true,
      ),
      draft(
        'Reopen a saved Case without losing its field values',
        `Reopening retains the original identifier, Subject, Working status, ${c.priority} priority, and ${c.owner} owner without creating another Case.`,
        'HAPPY_PATH',
        'AC-3',
        [
          ['Open the baseline Case.', 'The recorded identifier is displayed.'],
          [
            'Close its record tab.',
            'The record view closes; no business status transition is requested.',
          ],
          ['Search Cases by the recorded identifier.', 'Exactly that Case is listed.'],
          ['Open the listed record.', 'The same identifier appears.'],
          ['Inspect Subject.', 'The baseline synthetic subject is retained.', 'synthetic-subject'],
          ['Inspect Status.', 'Working is retained.'],
          ['Inspect Priority.', `${c.priority} is retained.`],
          ['Inspect Owner.', `${c.owner} is retained.`],
          [
            'Refresh the record page.',
            `The same identifier, Subject, Working, ${c.priority}, and ${c.owner} remain.`,
          ],
          [
            'Filter Cases by the exact subject.',
            'Exactly one matching Case exists.',
            'synthetic-subject',
          ],
        ],
        c,
        true,
      ),
      draft(
        'Reject a new Case with a blank Subject',
        'A clear Subject validation message blocks saving, and the isolated Case count remains unchanged.',
        'VALIDATION',
        'AC-4',
        [
          ['Select New in Cases.', 'The creation form appears.'],
          ['Leave Subject empty.', 'Subject contains no characters.'],
          [
            'Fill other owner-confirmed required fields using approved synthetic values.',
            'All other required inputs display the supplied values.',
          ],
          [
            'Select Save once.',
            'Submission is blocked and a clear Subject validation message appears.',
          ],
          [
            'Inspect the isolated Cases as the authorized observer.',
            'The total count equals the recorded baseline; no attempted record exists.',
          ],
        ],
        c,
      ),
      draft(
        'Prevent a read-only persona from creating a Case',
        'The read-only persona creates no Case; the isolated count remains unchanged and the attempted subject is absent.',
        'SECURITY',
        'AC-5',
        [
          ['Open Cases as the read-only persona.', 'Eligible existing Cases are visible.'],
          [
            'Inspect the New control.',
            'New is unavailable or disabled; if shown enabled, the normal creation attempt must be denied.',
          ],
          [
            'If New is enabled, open it and enter the unique synthetic subject.',
            'The input shows the synthetic subject; otherwise record that creation UI is unavailable.',
            'synthetic-subject',
          ],
          [
            'If the creation form is available, select Save once.',
            'Save is denied; record the exact denial or the unavailable control.',
          ],
          [
            'Inspect isolated Cases as the authorized observer.',
            'Count is unchanged and the unique attempted subject is absent.',
            'synthetic-subject',
          ],
        ],
        c,
        false,
        true,
      ),
      draft(
        'Prevent a read-only persona from updating a Case',
        'The original Case identifier and all baseline fields remain unchanged, with no duplicate.',
        'SECURITY',
        'AC-5',
        [
          [
            'Open the baseline Case as read-only.',
            'The recorded identifier and baseline field values are visible.',
          ],
          [
            'Inspect Edit.',
            'Edit is unavailable or disabled, or the normal update attempt is denied.',
          ],
          [
            'If Edit is enabled, attempt to change Status to New and select Save once.',
            'Submission is denied; record the denial or unavailable control.',
          ],
          [
            'Inspect Change Owner.',
            'Change Owner is unavailable or disabled, or the normal change attempt is denied.',
          ],
          [
            `If Change Owner is enabled, select ${c.owner === 'Synthetic Queue' ? 'Synthetic Agent' : 'Synthetic Queue'} and confirm once.`,
            'The owner change is denied; record the denial or unavailable control.',
          ],
          [
            'Open the same Case as the authorized observer.',
            'The original identifier is displayed.',
          ],
          ['Inspect Subject.', 'The baseline subject is unchanged.', 'synthetic-subject'],
          ['Inspect Status.', 'Working is unchanged.'],
          ['Inspect Priority.', `${c.priority} is unchanged.`],
          ['Inspect Owner.', `${c.owner} is unchanged.`],
          ['Filter by the exact subject.', 'The record count is unchanged.', 'synthetic-subject'],
        ],
        c,
        true,
        true,
      ),
    ],
  };
}
