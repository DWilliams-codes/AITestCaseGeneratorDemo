package com.testforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("testforge.demo")
public record DemoProperties(boolean seedEnabled) {}
