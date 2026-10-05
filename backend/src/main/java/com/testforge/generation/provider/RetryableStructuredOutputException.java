package com.testforge.generation.provider;

/** Marks incomplete, empty, or malformed structured output eligible for one controlled retry. */
public class RetryableStructuredOutputException extends GenerationProviderException {
  /** Preserves available usage from rejected transport output for aggregate accounting. */
  public RetryableStructuredOutputException(
      String message, Throwable cause, TestGenerationResult.UsageMetadata usage) {
    super(message, cause, usage);
  }

  /**
   * Initializes RetryableStructuredOutputException with its required collaborators and domain
   * state.
   */
  public RetryableStructuredOutputException(String message) {
    super(message);
  }

  /**
   * Initializes RetryableStructuredOutputException with its required collaborators and domain
   * state.
   */
  public RetryableStructuredOutputException(String message, Throwable cause) {
    super(message, cause);
  }
}
