package com.testforge.generation.provider;

/** Marks incomplete, empty, or malformed structured output eligible for one controlled retry. */
public class RetryableStructuredOutputException extends GenerationProviderException {
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
