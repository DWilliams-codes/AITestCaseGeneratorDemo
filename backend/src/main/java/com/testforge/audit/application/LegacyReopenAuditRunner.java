package com.testforge.audit.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Completes bounded bridge cleanup before the application advertises readiness. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyReopenAuditRunner implements ApplicationRunner {
  private final LegacyReopenAuditReconciler reconciler;

  /** Initializes the startup runner with the transactional batch reconciler. */
  public LegacyReopenAuditRunner(LegacyReopenAuditReconciler reconciler) {
    this.reconciler = reconciler;
  }

  /** Scrubs all pre-existing legacy rows through independently committed bounded batches. */
  @Override
  public void run(ApplicationArguments arguments) {
    while (reconciler.reconcileBatch() > 0) {
      // Each completed batch removes the reason marker and makes deterministic progress.
    }
  }
}
