package com.testforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.testforge.user.domain.UserEntity;
import com.testforge.user.repository.UserRepository;
import com.testforge.workspace.application.WorkspaceService;
import com.testforge.workspace.domain.WorkspaceMembershipEntity;
import com.testforge.workspace.domain.WorkspaceRole;
import com.testforge.workspace.domain.WorkspaceStatus;
import com.testforge.workspace.repository.WorkspaceMembershipRepository;
import com.testforge.workspace.repository.WorkspaceRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class WorkspaceReconciliationConcurrencyIntegrationTest {
  @Autowired private WorkspaceService workspaceService;
  @Autowired private UserRepository users;
  @Autowired private WorkspaceRepository workspaces;
  @Autowired private WorkspaceMembershipRepository memberships;
  @Autowired private PlatformTransactionManager transactionManager;

  /** Serializes simultaneous old-binary reconciliation into one deterministic tenancy. */
  @Test
  void concurrentReconciliationCreatesExactlyOneWorkspaceAndOwnerMembership() throws Exception {
    UserEntity user =
        users.saveAndFlush(
            UserEntity.create(
                "concurrent-reconciliation@example.test",
                "concurrent-reconciliation@example.test",
                "Concurrent Reconciliation",
                "not-a-real-password-hash",
                Instant.parse("2026-08-03T12:00:00Z")));
    UUID userId = user.getId();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch firstProvisionedWhileLocked = new CountDownLatch(1);
    CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
    CountDownLatch secondInvocationStarted = new CountDownLatch(1);
    TransactionTemplate transactions = new TransactionTemplate(transactionManager);
    Callable<UUID> firstReconciliation =
        () ->
            transactions.execute(
                transaction -> {
                  users.findByIdForPersonalWorkspaceReconciliation(userId).orElseThrow();
                  UUID reconciled = workspaceService.requirePersonalWorkspaceId(userId);
                  firstProvisionedWhileLocked.countDown();
                  try {
                    if (!releaseFirstTransaction.await(10, TimeUnit.SECONDS)) {
                      throw new IllegalStateException(
                          "First reconciliation transaction was not released in time.");
                    }
                  } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                        "First reconciliation transaction was interrupted.", exception);
                  }
                  return reconciled;
                });

    try {
      Future<UUID> first = executor.submit(firstReconciliation);
      assertThat(firstProvisionedWhileLocked.await(5, TimeUnit.SECONDS)).isTrue();
      Future<UUID> second =
          executor.submit(
              () -> {
                secondInvocationStarted.countDown();
                return workspaceService.requirePersonalWorkspaceId(userId);
              });
      assertThat(secondInvocationStarted.await(5, TimeUnit.SECONDS)).isTrue();
      assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
          .isInstanceOf(TimeoutException.class);
      releaseFirstTransaction.countDown();

      assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(userId);
      assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(userId);
    } finally {
      releaseFirstTransaction.countDown();
      executor.shutdownNow();
    }

    assertThat(workspaces.findAllById(List.of(userId))).hasSize(1);
    assertThat(memberships.countByWorkspaceIdAndUserId(userId, userId)).isEqualTo(1);
    WorkspaceMembershipEntity membership =
        memberships.findByWorkspaceIdAndUserId(userId, userId).orElseThrow();
    assertThat(membership.getId()).isEqualTo(userId);
    assertThat(membership.getCreatedBy()).isEqualTo(userId);
    assertThat(membership.getRole()).isEqualTo(WorkspaceRole.OWNER);
    assertThat(membership.getStatus()).isEqualTo(WorkspaceStatus.ACTIVE);
  }
}
