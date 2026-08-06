package com.testforge.export.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.audit.application.AuditMetadata;
import com.testforge.audit.application.AuditService;
import com.testforge.common.error.ApiExceptions;
import com.testforge.generation.application.ActiveGenerationSetResolver;
import com.testforge.generation.application.LegacyGenerationEvidenceReconciler;
import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.domain.RequirementEntity;
import com.testforge.testcase.application.TestCaseResponseAssembler;
import com.testforge.testcase.domain.TestCaseStatus;
import com.testforge.testcase.dto.TestCaseDtos.TestCaseResponse;
import com.testforge.testcase.repository.TestCaseRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {
  private final RequirementService requirementService;
  private final TestCaseRepository testCases;
  private final TestCaseResponseAssembler responseAssembler;
  private final ObjectMapper objectMapper;
  private final AuditService auditService;
  private final ActiveGenerationSetResolver activeSets;
  private final LegacyGenerationEvidenceReconciler legacyEvidence;
  private final MarkdownTextEncoder markdownTextEncoder;

  /** Initializes ExportService with its required collaborators and domain state. */
  public ExportService(
      RequirementService requirementService,
      TestCaseRepository testCases,
      TestCaseResponseAssembler responseAssembler,
      ObjectMapper objectMapper,
      AuditService auditService,
      ActiveGenerationSetResolver activeSets,
      LegacyGenerationEvidenceReconciler legacyEvidence,
      MarkdownTextEncoder markdownTextEncoder) {
    this.requirementService = requirementService;
    this.testCases = testCases;
    this.responseAssembler = responseAssembler;
    this.objectMapper = objectMapper;
    this.auditService = auditService;
    this.activeSets = activeSets;
    this.legacyEvidence = legacyEvidence;
    this.markdownTextEncoder = markdownTextEncoder;
  }

  /** Executes the export operation for ExportService. */
  @Transactional
  public ExportFile export(
      UUID ownerId, UUID requirementId, UUID generationRunId, String requestedFormat) {
    RequirementEntity requirement = requirementService.requireOwned(ownerId, requirementId);
    GenerationRunEntity selectedRun = selectReadableRun(requirementId, generationRunId);
    if (selectedRun != null) {
      legacyEvidence.reconcile(List.of(selectedRun));
    }
    UUID selectedRunId = selectedRun == null ? null : selectedRun.getId();
    List<TestCaseResponse> approved;
    if (selectedRunId == null) {
      approved = List.of();
    } else {
      approved =
          responseAssembler.assembleAll(
              testCases
                  .findAllByRequirementIdAndGenerationRunIdAndStatusOrderByWorkItemNumber(
                      requirementId, selectedRunId, TestCaseStatus.APPROVED, PageRequest.of(0, 100))
                  .getContent());
    }
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
        AuditMetadata.exported(format, approved.size()));
    return file;
  }

  /** Selects the active set by default and validates an explicit historical selector. */
  private GenerationRunEntity selectReadableRun(UUID requirementId, UUID requestedRunId) {
    if (requestedRunId == null) {
      return activeSets.resolve(requirementId).orElse(null);
    }
    return activeSets
        .successful(requirementId, requestedRunId)
        .orElseThrow(() -> ApiExceptions.notFound("Successful generation set not found."));
  }

  /** Maps the source data to json. */
  private String toJson(List<TestCaseResponse> approved) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(approved);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize the JSON export.", exception);
    }
  }

  /** Maps the source data to csv. */
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

  /** Maps the source data to markdown. */
  private String toMarkdown(RequirementEntity requirement, List<TestCaseResponse> approved) {
    StringBuilder markdown =
        new StringBuilder("# ")
            .append(markdownTextEncoder.encode(requirement.getTitle()))
            .append("\n\n");
    markdown.append("Approved manual test cases exported by TestForge AI.\n\n");
    for (TestCaseResponse testCase : approved) {
      markdown
          .append("## ")
          .append(markdownTextEncoder.encode(testCase.testCaseKey()))
          .append(" — ")
          .append(markdownTextEncoder.encode(testCase.title()))
          .append("\n\n")
          .append("- Category: ")
          .append(testCase.category())
          .append("\n- Priority: ")
          .append(testCase.priority())
          .append("\n- Acceptance criteria: ")
          .append(markdownTextEncoder.encode(String.join(", ", testCase.acceptanceCriteriaKeys())))
          .append("\n\n")
          .append(markdownTextEncoder.encode(testCase.objective()))
          .append("\n\n| Step | Action | Expected result |\n|---:|---|---|\n");
      testCase
          .steps()
          .forEach(
              step ->
                  markdown
                      .append('|')
                      .append(step.stepNumber())
                      .append('|')
                      .append(markdownTextEncoder.encode(step.action()))
                      .append('|')
                      .append(markdownTextEncoder.encode(step.expectedResult()))
                      .append("|\n"));
      markdown
          .append("\nFinal expected outcome: ")
          .append(markdownTextEncoder.encode(testCase.finalExpectedOutcome()))
          .append("\n\n");
    }
    return markdown.toString();
  }

  /** Executes the csv cell operation for ExportService. */
  private String csvCell(String value) {
    String safe = formulaSafe(value == null ? "" : value);
    return '"' + safe.replace("\"", "\"\"") + '"';
  }

  /** Executes the formula safe operation for ExportService. */
  private String formulaSafe(String value) {
    String stripped = value.stripLeading();
    if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) {
      return '\'' + value;
    }
    return value;
  }

  public record ExportFile(String mediaType, String extension, String content) {}
}
