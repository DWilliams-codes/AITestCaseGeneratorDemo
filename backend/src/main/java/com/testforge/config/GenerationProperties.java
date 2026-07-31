package com.testforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("testforge.generation")
public record GenerationProperties(String provider, int maximumCases, int maximumStepsPerCase) {}
