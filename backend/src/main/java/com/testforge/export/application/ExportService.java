package com.testforge.export.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.audit.application.AuditService;
import com.testforge.common.error.ApiExceptions;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.domain.RequirementEntity;
import com.testforge.testcase.application.TestCaseService;
import com.testforge.testcase.domain.TestCaseStatus;
import com.testforge.testcase.dto.TestCaseDtos.TestCaseResponse;
import com.testforge.testcase.repository.TestCaseRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {
  private final RequirementService requirementService;
  private final TestCaseRepository testCases;
  private final TestCaseService testCaseService;
  private final ObjectMapper objectMapper;
  private final AuditService auditService;

  public ExportService(
      RequirementService requirementService,
      TestCaseRepository testCases,
      TestCaseService testCaseService,
      ObjectMapper objectMapper,
      AuditService auditService) {
    this.requirementService = requirementService;
    this.testCases = testCases;
    this.testCaseService = testCaseService;
    this.objectMapper = objectMapper;
    this.auditService = auditService;
  }

  @Transactional
  public ExportFile export(UUID ownerId, UUID requirementId, String requestedFormat) {
    RequirementEntity requirement = requirementService.requireOwned(ownerId, requirementId);
    List<TestCaseResponse> approved =
        testCases
            .findAllByRequirementIdAndStatusOrderByTestCaseKey(
                requirementId, TestCaseStatus.APPROVED)
            .stream()
            .map(testCaseService::toResponse)
            .toList();
    if (approved.isEmpty()) {
      throw ApiExceptions.badRequest(
          "no_approved_test_cases", "Approve at least one test case before exporting.");
    }
    String format = requestedFormat.toLowerCase(Locale.ROOT);
    ExportFile file =
        switch (format) {
          case "csv" -> new ExportFile("text/csv", "csv", toCsv(approved));
          case "json" -> new ExportFile("application/json", "json", toJson(approved));
          case "markdown", "md" ->
              new ExportFile("text/markdown", "md", toMarkdown(requirement, approved));
          default ->
              throw ApiExceptions.badRequest(
                  "unsupported_export_format", "Format must be csv, json, or markdown.");
        };
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "REQUIREMENT",
        requirementId,
        "EXPORTED",
        Map.of("format", format, "approvedCaseCount", approved.size()));
    return file;
  }

  private String toJson(List<TestCaseResponse> approved) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(approved);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize the JSON export.", exception);
    }
  }

  private String toCsv(List<TestCaseResponse> approved) {
    StringBuilder csv =
        new StringBuilder(
            "testCaseKey,title,status,category,priority,acceptanceCriteria,stepNumber,action,expectedResult,finalExpectedOutcome\r\n");
    for (TestCaseResponse testCase : approved) {
      for (var step : testCase.steps()) {
        csv.append(csvCell(testCase.testCaseKey()))
            .append(',')
            .append(csvCell(testCase.title()))
            .append(',')
            .append(csvCell(testCase.status().name()))
            .append(',')
            .append(csvCell(testCase.category().name()))
            .append(',')
            .append(csvCell(testCase.priority().name()))
            .append(',')
            .append(csvCell(String.join(";", testCase.acceptanceCriteriaKeys())))
            .append(',')
            .append(step.stepNumber())
            .append(',')
            .append(csvCell(step.action()))
            .append(',')
            .append(csvCell(step.expectedResult()))
            .append(',')
            .append(csvCell(testCase.finalExpectedOutcome()))
            .append("\r\n");
      }
    }
    return csv.toString();
  }

  private String toMarkdown(RequirementEntity requirement, List<TestCaseResponse> approved) {
    StringBuilder markdown =
        new StringBuilder("# ").append(markdownText(requirement.getTitle())).append("\n\n");
    markdown.append("Approved manual test cases exported by TestForge AI.\n\n");
    for (TestCaseResponse testCase : approved) {
      markdown
          .append("## ")
          .append(markdownText(testCase.testCaseKey()))
          .append(" — ")
          .append(markdownText(testCase.title()))
          .append("\n\n")
          .append("- Category: ")
          .append(testCase.category())
          .append("\n- Priority: ")
          .append(testCase.priority())
          .append("\n- Acceptance criteria: ")
          .append(markdownText(String.join(", ", testCase.acceptanceCriteriaKeys())))
          .append("\n\n")
          .append(markdownText(testCase.objective()))
          .append("\n\n| Step | Action | Expected result |\n|---:|---|---|\n");
      testCase
          .steps()
          .forEach(
              step ->
                  markdown
                      .append('|')
                      .append(step.stepNumber())
                      .append('|')
                      .append(markdownText(step.action()))
                      .append('|')
                      .append(markdownText(step.expectedResult()))
                      .append("|\n"));
      markdown
          .append("\nFinal expected outcome: ")
          .append(markdownText(testCase.finalExpectedOutcome()))
          .append("\n\n");
    }
    return markdown.toString();
  }

  private String csvCell(String value) {
    String safe = formulaSafe(value == null ? "" : value);
    return '"' + safe.replace("\"", "\"\"") + '"';
  }

  private String formulaSafe(String value) {
    String stripped = value.stripLeading();
    if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) {
      return '\'' + value;
    }
    return value;
  }

  private String markdownText(String value) {
    return (value == null ? "" : value)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("|", "\\|")
        .replace("\r", " ")
        .replace("\n", " ");
  }

  public record ExportFile(String mediaType, String extension, String content) {}
}
