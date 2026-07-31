package com.testforge.export.controller;

import com.testforge.export.application.ExportService;
import com.testforge.export.application.ExportService.ExportFile;
import com.testforge.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requirements/{requirementId}/export")
public class ExportController {
  private final ExportService exportService;
  private final CurrentUser currentUser;

  public ExportController(ExportService exportService, CurrentUser currentUser) {
    this.exportService = exportService;
    this.currentUser = currentUser;
  }

  @GetMapping
  ResponseEntity<String> export(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @RequestParam String format) {
    ExportFile file = exportService.export(currentUser.id(authentication), requirementId, format);
    ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename("testforge-" + requirementId + '.' + file.extension(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.mediaType() + ";charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(file.content());
  }
}
