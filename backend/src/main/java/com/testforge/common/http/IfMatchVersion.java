package com.testforge.common.http;

import com.testforge.common.error.ApiExceptions;

/** Parses one strong, quoted numeric ETag used for aggregate concurrency control. */
public final class IfMatchVersion {
  /** Creates an empty IfMatchVersion instance for framework-managed construction. */
  private IfMatchVersion() {}

  /** Executes the parse operation for IfMatchVersion. */
  public static long parse(String header) {
    if (header == null || !header.matches("\"(?:0|[1-9][0-9]*)\"")) {
      throw ApiExceptions.badRequest(
          "invalid_if_match", "If-Match must contain one strong quoted numeric version.");
    }
    try {
      return Long.parseLong(header.substring(1, header.length() - 1));
    } catch (NumberFormatException exception) {
      throw ApiExceptions.badRequest(
          "invalid_if_match", "If-Match contains a version outside the supported range.");
    }
  }
}
