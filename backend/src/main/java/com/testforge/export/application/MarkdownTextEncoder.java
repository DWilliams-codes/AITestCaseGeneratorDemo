package com.testforge.export.application;

import org.springframework.stereotype.Component;

/** Encodes untrusted text so Markdown parses it only as inert visible content. */
@Component
public class MarkdownTextEncoder {
  /** Executes the encode operation for MarkdownTextEncoder. */
  public String encode(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder encoded = new StringBuilder();
    value
        .codePoints()
        .forEach(
            codePoint -> {
              if (codePoint == '\r' || codePoint == '\n') {
                encoded.append(' ');
              } else if (Character.isLetterOrDigit(codePoint) || codePoint == ' ') {
                encoded.appendCodePoint(codePoint);
              } else {
                encoded.append("&#").append(codePoint).append(';');
              }
            });
    return encoded.toString();
  }
}
