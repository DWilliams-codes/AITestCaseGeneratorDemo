package com.testforge.generation.provider;

public interface TestGenerationProvider {
  /** Generates structured manual test coverage from the requirement input. */
  TestGenerationResult generate(TestGenerationRequest request);

  /** Returns the stable provider identifier stored with generation runs. */
  default String providerName() {
    return getClass().getSimpleName();
  }

  /** Returns the model or engine identifier stored with generation runs. */
  default String modelName() {
    return "unspecified";
  }
}
