package com.testforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.testforge.audit.repository.AuditEventRepository;
import com.testforge.auth.application.AuthService;
import com.testforge.auth.domain.RefreshTokenEntity;
import com.testforge.auth.dto.AuthDtos.RegisterRequest;
import com.testforge.auth.repository.RefreshTokenRepository;
import com.testforge.user.repository.UserRepository;
import com.testforge.workspace.repository.WorkspaceMembershipRepository;
import com.testforge.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
class RegistrationAtomicityIntegrationTest {
  @Autowired private AuthService authService;
  @Autowired private UserRepository users;
  @Autowired private WorkspaceRepository workspaces;
  @Autowired private WorkspaceMembershipRepository memberships;
  @Autowired private AuditEventRepository auditEvents;
  @MockitoSpyBean private RefreshTokenRepository refreshTokens;

  /** Rolls back user and personal-tenancy writes when late session persistence fails. */
  @Test
  void rollsBackCompleteRegistrationAggregateWhenRefreshTokenSaveFails() throws Exception {
    long usersBefore = users.count();
    long workspacesBefore = workspaces.count();
    long membershipsBefore = memberships.count();
    long refreshTokensBefore = refreshTokens.count();
    long auditEventsBefore = auditEvents.count();
    doThrow(new DataIntegrityViolationException("forced refresh-token failure"))
        .when(refreshTokens)
        .save(any(RefreshTokenEntity.class));

    assertThatThrownBy(
            () ->
                authService.register(
                    new RegisterRequest(
                        "atomicity@example.test",
                        "Atomic Registration",
                        "TestForge!Atomicity2026")))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessage("forced refresh-token failure");

    assertThat(users.findByEmailNormalized("atomicity@example.test")).isEmpty();
    assertThat(users.count()).isEqualTo(usersBefore);
    assertThat(workspaces.count()).isEqualTo(workspacesBefore);
    assertThat(memberships.count()).isEqualTo(membershipsBefore);
    assertThat(refreshTokens.count()).isEqualTo(refreshTokensBefore);
    assertThat(auditEvents.count()).isEqualTo(auditEventsBefore);
  }
}
