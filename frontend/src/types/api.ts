export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';
export type RequirementStatus =
  'DRAFT' | 'READY_FOR_GENERATION' | 'GENERATED' | 'NEEDS_CLARIFICATION' | 'ARCHIVED';
export type TestCaseStatus = 'GENERATED' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED' | 'NEEDS_REVISION';
export type TestPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type TestCaseCategory =
  | 'HAPPY_PATH'
  | 'NEGATIVE'
  | 'BOUNDARY'
  | 'VALIDATION'
  | 'PERMISSION'
  | 'DATA_INTEGRITY'
  | 'ERROR_HANDLING'
  | 'ACCESSIBILITY'
  | 'INTEGRATION'
  | 'SECURITY'
  | 'RECOVERY'
  | 'OTHER';

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface User {
  id: string;
  email: string;
  displayName: string;
  role: string;
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  expiresInSeconds: number;
  user: User;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  status: ProjectStatus;
  requirementCount: number;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface RequirementSummary {
  id: string;
  projectId: string;
  title: string;
  status: RequirementStatus;
  acceptanceCriteriaCount: number;
  updatedAt: string;
  version: number;
}

export interface AcceptanceCriterion {
  id: string;
  criterionKey: string;
  description: string;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface Ambiguity {
  id: string;
  category: string;
  severity: TestPriority;
  description: string;
  suggestedQuestion: string;
  resolved: boolean;
  resolution: string | null;
  createdAt: string;
  resolvedAt: string | null;
  version: number;
}

export interface Requirement extends RequirementSummary {
  userStory: string;
  businessRequirements: string;
  assumptions: string;
  sourceReference: string;
  acceptanceCriteria: AcceptanceCriterion[];
  ambiguities: Ambiguity[];
  createdAt: string;
}

export interface TestStep {
  stepNumber: number;
  action: string;
  expectedResult: string;
  testDataReference: string | null;
}

export interface TestCase {
  id: string;
  requirementId: string;
  generationRunId: string;
  testCaseKey: string;
  title: string;
  objective: string;
  category: TestCaseCategory;
  priority: TestPriority;
  riskLevel: TestPriority;
  automationCandidate: boolean;
  status: TestCaseStatus;
  coverageIntent: 'ACCEPTANCE_CRITERIA' | 'SUPPORTING_EXPLORATORY';
  rationale: string;
  finalExpectedOutcome: string;
  preconditions: { sortOrder: number; description: string }[];
  steps: TestStep[];
  testData: {
    name: string;
    description: string;
    exampleValue: string;
    sensitivity: 'PUBLIC' | 'INTERNAL' | 'CONFIDENTIAL' | 'RESTRICTED';
    generationStrategy: string;
  }[];
  acceptanceCriteriaKeys: string[];
  reviews: {
    id: string;
    reviewerId: string;
    decision: string;
    comments: string;
    createdAt: string;
  }[];
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface Coverage {
  requirementId: string;
  totalCriteria: number;
  coveredCriteria: number;
  approvedCriteria: number;
  coveragePercent: number;
  approvedCoveragePercent: number;
}

export interface Traceability {
  requirementId: string;
  rows: {
    acceptanceCriterionId: string;
    criterionKey: string;
    description: string;
    testCases: {
      id: string;
      testCaseKey: string;
      title: string;
      status: TestCaseStatus;
      coverageType: 'DIRECT' | 'PARTIAL' | 'SUPPORTING';
      confidence: number;
    }[];
  }[];
}
