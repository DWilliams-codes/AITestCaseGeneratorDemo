package com.testforge.generation.provider;

public class GenerationProviderException extends RuntimeException {
  private final TestGenerationResult.UsageMetadata usage;

  /** Carries only sanitized transport counts across the provider failure boundary. */
  public GenerationProviderException(
      String message, Throwable cause, TestGenerationResult.UsageMetadata usage) {
    super(message, cause);
    this.usage = usage;
  }

  /** Returns known provider transport usage without retaining response bodies. */
  public TestGenerationResult.UsageMetadata usage() {
    return usage;
  }

  /** Initializes GenerationProviderException with its required collaborators and domain state. */
  public GenerationProviderException(String message) {
    this(message, null, null);
  }

  /** Initializes GenerationProviderException with its required collaborators and domain state. */
  public GenerationProviderException(String message, Throwable cause) {
    this(message, cause, null);
  }
}
