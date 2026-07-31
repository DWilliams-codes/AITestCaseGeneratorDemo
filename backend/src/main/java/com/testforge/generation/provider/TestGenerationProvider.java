package com.testforge.generation.provider;

public interface TestGenerationProvider {
  TestGenerationResult generate(TestGenerationRequest request);

  default String providerName() {
    return getClass().getSimpleName();
  }

  default String modelName() {
    return "unspecified";
  }
}
