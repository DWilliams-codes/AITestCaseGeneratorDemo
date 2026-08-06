package com.testforge.export.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownTextEncoderTest {
  private final MarkdownTextEncoder encoder = new MarkdownTextEncoder();

  /** Keeps the complete hostile Markdown corpus inert while retaining visible text. */
  @Test
  void encodesMarkdownHtmlProtocolsReferencesAndMultilinePayloads() {
    List<String> payloads =
        List.of(
            "[click](javascript:alert(1))",
            "![image](https://attacker.test/pixel)",
            "<script>alert(1)</script>",
            "<https://attacker.test>",
            "[ref]: https://attacker.test",
            "`inline` and ```fence```",
            "javascript:alert(1)",
            "first\n# heading\r\n- list",
            "\\* escaped emphasis");

    for (String payload : payloads) {
      String encoded = encoder.encode(payload);
      assertThat(encoded)
          .doesNotContain("[", "]", "(", ")", "!", "<", ">", "`", "*", "\\")
          .doesNotContain("# heading")
          .doesNotContain("\r", "\n");
    }
  }

  /** Preserves ordinary alphanumeric text and safely handles null. */
  @Test
  void preservesPlainTextAndEncodesPunctuation() {
    assertThat(encoder.encode("Case 42: safe.")).isEqualTo("Case 42&#58; safe&#46;");
    assertThat(encoder.encode(null)).isEmpty();
  }
}
