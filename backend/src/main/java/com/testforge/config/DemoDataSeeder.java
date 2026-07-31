package com.testforge.config;

import com.testforge.auth.application.AuthService;
import com.testforge.auth.dto.AuthDtos.RegisterRequest;
import com.testforge.generation.application.GenerationService;
import com.testforge.project.application.ProjectService;
import com.testforge.project.dto.ProjectDtos.CreateProjectRequest;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.dto.RequirementDtos.CreateRequirementRequest;
import com.testforge.testcase.application.TestCaseService;
import com.testforge.testcase.domain.ReviewDecision;
import com.testforge.testcase.dto.TestCaseDtos.ReviewRequest;
import com.testforge.user.repository.UserRepository;
import java.util.List;
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
  private final RequirementService requirementService;
  private final GenerationService generationService;
  private final TestCaseService testCaseService;

  public DemoDataSeeder(
      UserRepository users,
      AuthService authService,
      ProjectService projectService,
      RequirementService requirementService,
      GenerationService generationService,
      TestCaseService testCaseService) {
    this.users = users;
    this.authService = authService;
    this.projectService = projectService;
    this.requirementService = requirementService;
    this.generationService = generationService;
    this.testCaseService = testCaseService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (users.existsByEmailNormalized(DEMO_EMAIL)) {
      return;
    }
    var session = authService.register(new RegisterRequest(DEMO_EMAIL, "Maya Chen", DEMO_PASSWORD));
    var userId = session.response().user().id();
    var project =
        projectService.create(
            userId,
            new CreateProjectRequest(
                "Customer Returns Portal",
                "Stage 1 QA coverage for a retail returns workflow and its policy controls."));
    var requirement =
        requirementService.create(
            userId,
            project.id(),
            new CreateRequirementRequest(
                "Submit an eligible product return",
                "As a signed-in customer, I want to submit a return request from the order details page, so that I can receive a refund for an eligible purchase.",
                "The return form requires a reason and a refund method. Items are eligible within 30 calendar days of delivery. The order owner may submit the return. Duplicate requests for the same order item must be blocked. If the refund service is unavailable, preserve the entered values and allow a retry.",
                "The demo environment contains a delivered test order and synthetic payment data.",
                "DEMO-RET-101",
                List.of(
                    "Given an eligible delivered item, when its owner submits a reason and refund method, then one return request is created.",
                    "An item delivered exactly 30 calendar days ago remains eligible; an item delivered 31 days ago is rejected.",
                    "A second return request for the same order item is rejected without creating a duplicate.",
                    "If the refund service is unavailable, the form preserves entered values and permits a single retry.")));
    generationService.generate(userId, requirement.id(), "demo-seed-generation-v1");
    testCaseService.list(userId, requirement.id()).stream()
        .limit(3)
        .forEach(
            testCase ->
                testCaseService.review(
                    userId,
                    testCase.id(),
                    ReviewDecision.APPROVED,
                    new ReviewRequest("Approved for the portfolio demo.")));
  }
}
