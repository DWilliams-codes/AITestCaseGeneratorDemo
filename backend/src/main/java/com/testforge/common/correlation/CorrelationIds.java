package com.testforge.common.correlation;

import org.slf4j.MDC;

public final class CorrelationIds {
  public static final String HEADER = "X-Correlation-ID";
  public static final String MDC_KEY = "correlationId";

  private CorrelationIds() {}

  public static String current() {
    String value = MDC.get(MDC_KEY);
    return value == null ? "unavailable" : value;
  }
}
