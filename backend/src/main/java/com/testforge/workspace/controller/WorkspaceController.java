package com.testforge.workspace.controller;

import com.testforge.security.CurrentUser;
import com.testforge.workspace.application.WorkspaceService;
import com.testforge.workspace.dto.WorkspaceDtos.WorkspaceResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {
  private final WorkspaceService workspaceService;
  private final CurrentUser currentUser;

  /** Initializes the workspace HTTP boundary with its scoped application service. */
  public WorkspaceController(WorkspaceService workspaceService, CurrentUser currentUser) {
    this.workspaceService = workspaceService;
    this.currentUser = currentUser;
  }

  /** Lists the authenticated caller's workspace memberships and roles. */
  @GetMapping
  List<WorkspaceResponse> list(Authentication authentication) {
    return workspaceService.list(currentUser.id(authentication));
  }
}
