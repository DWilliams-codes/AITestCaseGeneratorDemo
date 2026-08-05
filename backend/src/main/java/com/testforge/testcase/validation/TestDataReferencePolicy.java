package com.testforge.testcase.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Shared normalization and referential-integrity policy for per-case test data. */
@Component
public class TestDataReferencePolicy {

  /** Validates unique names and returns each reference in its canonical stored spelling. */
  public List<String> canonicalize(List<String> names, List<String> references) {
    Map<String, String> canonicalByKey = new LinkedHashMap<>();
    for (String name : names) {
      if (name == null || name.isBlank()) {
        throw new Violation("Test-data names must not be blank.");
      }
      String canonical = name.strip();
      String key = key(canonical);
      if (canonicalByKey.putIfAbsent(key, canonical) != null) {
        throw new Violation(
            "Test-data names must be unique after trimming and case normalization.");
      }
    }
    return references.stream()
        .map(
            reference -> {
              if (reference == null || reference.isBlank()) {
                return null;
              }
              String canonical = canonicalByKey.get(key(reference.strip()));
              if (canonical == null) {
                throw new Violation(
                    "Every step test-data reference must resolve to a named item in the same test case.");
              }
              return canonical;
            })
        .toList();
  }

  /** Returns a normalized identifier used only for case-insensitive matching. */
  public String key(String value) {
    return value.strip().toLowerCase(Locale.ROOT);
  }

  /** A domain-level violation translated at the generation or API boundary. */
  public static final class Violation extends IllegalArgumentException {
    /** Creates a policy violation with a safe validation message. */
    public Violation(String message) {
      super(message);
    }
  }
}
