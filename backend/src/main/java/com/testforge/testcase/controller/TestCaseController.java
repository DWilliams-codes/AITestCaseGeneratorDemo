package com.testforge.testcase.controller;

import com.testforge.common.dto.PageResponse;
import com.testforge.security.CurrentUser;
import com.testforge.testcase.application.TestCaseService;
import com.testforge.testcase.domain.ReviewDecision;
import com.testforge.testcase.dto.TestCaseDtos.ReopenRequest;
import com.testforge.testcase.dto.TestCaseDtos.ReviewRequest;
import com.testforge.testcase.dto.TestCaseDtos.RevisionResponse;
import com.testforge.testcase.dto.TestCaseDtos.TestCaseResponse;
import com.testforge.testcase.dto.TestCaseDtos.UpdateTestCaseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TestCaseController {
  private final TestCaseService testCaseService;
  private final CurrentUser currentUser;

  /** Initializes TestCaseController with its required collaborators and domain state. */
  public TestCaseController(TestCaseService testCaseService, CurrentUser currentUser) {
    this.testCaseService = testCaseService;
    this.currentUser = currentUser;
  }

  /** Handles the authenticated HTTP request to list. */
  @GetMapping({
    "/user-stories/{requirementId}/test-cases",
    "/requirements/{requirementId}/test-cases"
  })
  List<TestCaseResponse> list(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @RequestParam(required = false) UUID generationRunId) {
    return testCaseService.list(currentUser.id(authentication), requirementId, generationRunId);
  }

  /** Handles the authenticated HTTP request to get. */
  @GetMapping("/test-cases/{testCaseId}")
  TestCaseResponse get(Authentication authentication, @PathVariable UUID testCaseId) {
    return testCaseService.get(currentUser.id(authentication), testCaseId);
  }

  /** Handles the authenticated HTTP request to update. */
  @PatchMapping("/test-cases/{testCaseId}")
  TestCaseResponse update(
      Authentication authentication,
      @PathVariable UUID testCaseId,
      @Valid @RequestBody UpdateTestCaseRequest request) {
    return testCaseService.update(currentUser.id(authentication), testCaseId, request);
  }

  /** Handles the authenticated HTTP request to approve. */
  @PostMapping("/test-cases/{testCaseId}/approve")
  TestCaseResponse approve(
      Authentication authentication,
      @PathVariable UUID testCaseId,
      @Valid @RequestBody ReviewRequest request) {
    return review(authentication, testCaseId, request, ReviewDecision.APPROVED);
  }

  /** Handles the authenticated HTTP request to reject. */
  @PostMapping("/test-cases/{testCaseId}/reject")
  TestCaseResponse reject(
      Authentication authentication,
      @PathVariable UUID testCaseId,
      @Valid @RequestBody ReviewRequest request) {
    return review(authentication, testCaseId, request, ReviewDecision.REJECTED);
  }

  /** Handles the authenticated HTTP request to request changes. */
  @PostMapping("/test-cases/{testCaseId}/request-changes")
  TestCaseResponse requestChanges(
      Authentication authentication,
      @PathVariable UUID testCaseId,
      @Valid @RequestBody ReviewRequest request) {
    return review(authentication, testCaseId, request, ReviewDecision.CHANGES_REQUESTED);
  }

  /** Reopens a terminal test case with an explicit reason. */
  @PostMapping("/test-cases/{testCaseId}/reopen")
  TestCaseResponse reopen(
      Authentication authentication,
      @PathVariable UUID testCaseId,
      @Valid @RequestBody ReopenRequest request) {
    return testCaseService.reopen(currentUser.id(authentication), testCaseId, request);
  }

  /** Returns a bounded page of normalized revisions. */
  @GetMapping("/test-cases/{testCaseId}/revisions")
  PageResponse<RevisionResponse> revisions(
      Authentication authentication,
      @PathVariable UUID testCaseId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return testCaseService.revisions(currentUser.id(authentication), testCaseId, page, size);
  }

  /** Handles the authenticated HTTP request to review. */
  private TestCaseResponse review(
      Authentication authentication,
      UUID testCaseId,
      ReviewRequest request,
      ReviewDecision decision) {
    return testCaseService.review(currentUser.id(authentication), testCaseId, decision, request);
  }
}
