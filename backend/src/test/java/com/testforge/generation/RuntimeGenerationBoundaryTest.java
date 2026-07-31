package com.testforge.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RuntimeGenerationBoundaryTest {
  /** Prevents canned test cases or step sequences from returning to production source code. */
  @Test
  void productionRuntimeContainsNoConstructedGenerationOutput() throws IOException {
    String productionSource = readJavaSources(Path.of("src/main/java"));

    assertThat(productionSource)
        .doesNotContain("new GeneratedTestCase(")
        .doesNotContain("new GeneratedStep(")
        .doesNotContain("class FakeTestGenerationProvider");
  }

  /** Ensures demo startup seeds source requirements without invoking the generation workflow. */
  @Test
  void demoSeederContainsOnlyGenerationInputs() throws IOException {
    String seeder =
        Files.readString(Path.of("src/main/java/com/testforge/config/DemoDataSeeder.java"));

    assertThat(seeder)
        .doesNotContain("GenerationService")
        .doesNotContain("TestCaseService")
        .doesNotContain("GeneratedTestCase")
        .doesNotContain("GeneratedStep");
  }

  /**
   * Concatenates maintained Java sources so architectural assertions inspect the complete runtime.
   */
  private String readJavaSources(Path root) throws IOException {
    StringBuilder result = new StringBuilder();
    try (var sources = Files.walk(root)) {
      for (Path source :
          sources.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
        result.append(Files.readString(source)).append('\n');
      }
    }
    return result.toString();
  }
}
