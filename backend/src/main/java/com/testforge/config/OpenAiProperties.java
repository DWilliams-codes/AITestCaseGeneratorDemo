package com.testforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("testforge.generation.openai")
public record OpenAiProperties(
    String apiKey,
    String baseUrl,
    String model,
    int connectTimeoutSeconds,
    int readTimeoutSeconds,
    int maximumOutputTokens) {}
