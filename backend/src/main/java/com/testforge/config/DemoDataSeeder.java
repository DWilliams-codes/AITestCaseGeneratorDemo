package com.testforge.config;

import com.testforge.auth.application.AuthService;
import com.testforge.auth.dto.AuthDtos.RegisterRequest;
import com.testforge.project.application.ProjectService;
import com.testforge.project.dto.ProjectDtos.CreateProjectRequest;
import com.testforge.project.repository.ProjectRepository;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.dto.RequirementDtos.CreateRequirementRequest;
import com.testforge.requirement.repository.RequirementRepository;
import com.testforge.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "testforge.demo.seed-enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
  public static final String DEMO_EMAIL = "demo@testforge.local";
  public static final String DEMO_PASSWORD = "TestForge!Demo2026";

  private final UserRepository users;
  private final AuthService authService;
  private final ProjectService projectService;
  private final ProjectRepository projects;
  private final RequirementService requirementService;
  private final RequirementRepository requirements;

  /** Initializes DemoDataSeeder with its required collaborators and domain state. */
  public DemoDataSeeder(
      UserRepository users,
      AuthService authService,
      ProjectService projectService,
      ProjectRepository projects,
      RequirementService requirementService,
      RequirementRepository requirements) {
    this.users = users;
    this.authService = authService;
    this.projectService = projectService;
    this.projects = projects;
    this.requirementService = requirementService;
    this.requirements = requirements;
  }

  /** Ensures the demo account contains professional source stories without generated output. */
  @Override
  public void run(ApplicationArguments args) {
    UUID userId = ensureDemoUser();
    UUID projectId = ensureDemoProject(userId);
    professionalStories().stream()
        .filter(
            story ->
                !requirements.existsByProjectIdAndSourceReference(
                    projectId, story.sourceReference()))
        .forEach(story -> createStory(userId, projectId, story));
  }

  /** Returns the existing demo account or creates it on the first demo startup. */
  private UUID ensureDemoUser() {
    return users
        .findByEmailNormalized(DEMO_EMAIL)
        .map(user -> user.getId())
        .orElseGet(
            () ->
                authService
                    .register(new RegisterRequest(DEMO_EMAIL, "Maya Chen", DEMO_PASSWORD))
                    .response()
                    .user()
                    .id());
  }

  /** Returns the stable demo project or creates it when the account has no matching project. */
  private UUID ensureDemoProject(UUID userId) {
    return projects
        .findFirstByOwnerIdAndNameOrderByCreatedAtAsc(userId, "Commerce Returns Platform")
        .map(project -> project.getId())
        .orElseGet(
            () ->
                projectService
                    .create(
                        userId,
                        new CreateProjectRequest(
                            "Commerce Returns Platform",
                            "Enterprise-grade return initiation, exception review, tracking, and cancellation workflows."))
                    .id());
  }

  /** Persists one source story and its acceptance criteria without creating test artifacts. */
  private void createStory(UUID userId, UUID projectId, DemoStory story) {
    requirementService.create(
        userId,
        projectId,
        new CreateRequirementRequest(
            story.title(),
            story.userStory(),
            story.businessRequirements(),
            story.assumptions(),
            story.sourceReference(),
            story.acceptanceCriteria()));
  }

  /** Provides representative enterprise stories used solely as inputs to live generation. */
  private List<DemoStory> professionalStories() {
    return List.of(
        new DemoStory(
            "Submit an eligible product return",
            "As an authenticated purchaser, I want to submit a return request for an eligible order item from Order Details so that I can select a supported resolution without contacting Customer Support.",
            "Only the purchaser or an authorized account delegate may initiate the return. Eligibility is calculated from the delivery timestamp using the marketplace timezone: items delivered 30 calendar days ago are eligible and items delivered 31 days ago are not. The customer must provide a return reason, requested quantity, and supported resolution. Requested quantity cannot exceed the unreturned quantity. Submission must be idempotent for the same account, order item, quantity, and client request identifier. If the returns service is unavailable, no return is created, entered values remain available, and the customer may retry safely. Error messages must not expose internal service details or payment data.",
            "The test environment provides synthetic accounts with purchaser and delegate roles, delivered orders at configurable boundary dates, refundable quantities, and controllable dependency responses.",
            "ADO-RET-2101",
            List.of(
                "Given an eligible delivered item owned by the authenticated purchaser, when the purchaser submits a supported reason, quantity, and resolution, then exactly one return request is created and its confirmation identifier is displayed.",
                "Given an authorized account delegate, when the delegate submits an otherwise valid return, then the return is created and the initiating delegate is recorded in the audit history.",
                "Given an item delivered exactly 30 calendar days earlier in the marketplace timezone, when eligibility is evaluated, then the item remains returnable; an item delivered 31 calendar days earlier is rejected as outside the return window.",
                "Given a quantity greater than the unreturned quantity, when the request is submitted, then submission is blocked with a field-level message and no return record is created.",
                "Given two submissions with the same client request identifier and return payload, when both are processed, then both responses reference the same return and only one return record exists.",
                "Given the returns service is unavailable, when the customer submits a valid request, then no partial return is created, entered values are preserved, and a later retry can complete once without duplication.")),
        new DemoStory(
            "Track return and refund progress",
            "As a customer with an active return, I want to view its current return and refund status so that I understand what has completed and whether I need to take action.",
            "The timeline must display status events in ascending event time and distinguish carrier, warehouse, and refund milestones. Status changes must be visible within 60 seconds of an accepted upstream event. Customers may view only returns belonging to their account. A malformed or unauthorized return identifier must produce the same not-found experience. Personally identifiable and payment data must be masked. When status data is temporarily unavailable, the last confirmed status remains visible with its timestamp and a non-destructive retry action. The timeline and retry control must meet WCAG 2.2 AA keyboard and screen-reader requirements.",
            "The test environment can publish synthetic return lifecycle events, advance time, simulate delayed and duplicate events, and toggle the status service availability.",
            "ADO-RET-2102",
            List.of(
                "Given an owned return with carrier, warehouse, and refund events, when the customer opens its details, then each event is shown once in ascending event-time order with its source and timestamp.",
                "Given an accepted upstream status event, when 60 seconds have elapsed, then the corresponding status is visible without requiring the customer to sign in again.",
                "Given a return owned by another account or an unknown return identifier, when the customer requests it, then the application returns the same not-found response and reveals no ownership information.",
                "Given status data is temporarily unavailable, when the page is opened, then the last confirmed status and timestamp remain visible, no false milestone is shown, and Retry can recover the view without duplicating events.",
                "Given a keyboard-only user or supported screen reader, when the timeline and Retry control are used, then focus order, accessible names, status announcements, and contrast satisfy WCAG 2.2 AA expectations.",
                "Given timeline data containing customer and payment attributes, when it is rendered, then only approved masked values are displayed and raw sensitive values are absent from the page and client logs.")),
        new DemoStory(
            "Approve a high-value return exception",
            "As a Returns Operations approver, I want to review high-value policy exceptions with independent approval controls so that legitimate exceptions can be resolved without weakening financial governance.",
            "Only users with the Returns Approver role may decide an exception. Requests with an expected refund of 500 USD or more require an approver who is different from the requester. The decision requires a reason code and a comment between 20 and 1,000 characters. Approval and rejection are final state transitions and must use optimistic concurrency so a stale browser cannot overwrite a completed decision. Every view and decision must create an immutable audit event containing actor, timestamp, request version, decision, and correlation identifier, without storing payment credentials. If the audit service cannot accept the event, the decision must fail atomically and remain pending.",
            "The test environment includes synthetic requester, approver, and unauthorized identities; configurable refund amounts; controllable record versions; and an audit-service failure switch.",
            "ADO-RET-2103",
            List.of(
                "Given a pending exception below 500 USD, when an authorized approver who also created the request approves it with valid decision details, then the exception is approved and the audit event identifies the same actor as requester and approver.",
                "Given a pending exception of exactly 500 USD, when its requester attempts to approve it, then the decision is blocked and the exception remains pending.",
                "Given a pending exception of at least 500 USD, when a different authorized approver submits a reason code and a 20-to-1,000-character comment, then the decision succeeds once and an immutable audit event records the approved version.",
                "Given an unauthorized user, when the user attempts to view or decide an exception, then access is denied without exposing the exception's financial or customer details.",
                "Given two approvers loaded the same pending version, when one completes a decision before the other, then the second receives a stale-version conflict and cannot replace the recorded decision.",
                "Given the audit service rejects the audit event, when an otherwise valid decision is submitted, then the decision and audit write are rolled back together and the exception remains pending.")),
        new DemoStory(
            "Cancel a return before carrier acceptance",
            "As a customer who no longer wants to return an item, I want to cancel an eligible return before the carrier accepts the package so that the order item is released from the return workflow.",
            "A customer may cancel only a return owned by the customer's account and only while its state is REQUESTED or LABEL_CREATED. Cancellation is no longer allowed after the first carrier-accepted event, even if that event arrives while the cancellation request is processing. The operation requires the current return version and an idempotency key. A successful cancellation invalidates any unused label and restores the refundable quantity exactly once. If label invalidation fails, the cancellation must not commit and the customer must receive a retryable, non-sensitive error. Duplicate cancellation requests must return the original result without repeating downstream side effects.",
            "The test environment supports synthetic return states, concurrent carrier events, version conflicts, duplicate idempotency keys, and controllable label-service outcomes.",
            "ADO-RET-2104",
            List.of(
                "Given an owned return in REQUESTED or LABEL_CREATED state with the current version, when the customer cancels it, then the state becomes CANCELLED, refundable quantity is restored once, and any unused label is invalidated.",
                "Given a return with a carrier-accepted event, when cancellation is attempted, then cancellation is rejected, the state is unchanged, and refundable quantity is not restored.",
                "Given carrier acceptance and cancellation are processed concurrently, when the carrier event commits first, then cancellation detects the changed version or state and cannot overwrite carrier acceptance.",
                "Given a stale return version, when cancellation is submitted, then a conflict response is returned and no state, quantity, or label side effect occurs.",
                "Given label invalidation fails, when cancellation is submitted, then the transaction remains uncommitted, the return stays eligible for retry, and no internal dependency detail is exposed.",
                "Given repeated cancellation requests with the same idempotency key, when they are processed, then each response reports the same result and downstream label and quantity operations execute no more than once.")));
  }

  /** Defines one immutable source story fixture; no generated test output is stored here. */
  private record DemoStory(
      String title,
      String userStory,
      String businessRequirements,
      String assumptions,
      String sourceReference,
      List<String> acceptanceCriteria) {}
}
