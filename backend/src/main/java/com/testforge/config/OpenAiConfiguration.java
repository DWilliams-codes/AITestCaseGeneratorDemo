package com.testforge.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "testforge.generation.provider", havingValue = "openai")
public class OpenAiConfiguration {
  /** Creates the Spring-managed open ai rest client component. */
  @Bean
  @Qualifier("openAiRestClient") RestClient openAiRestClient(OpenAiProperties properties) {
    if (!StringUtils.hasText(properties.apiKey())) {
      throw new IllegalStateException(
          "OPENAI_API_KEY is required when TEST_GENERATION_PROVIDER=openai.");
    }
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
    requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .defaultHeader("Authorization", "Bearer " + properties.apiKey())
        .defaultHeader("Content-Type", "application/json")
        .requestFactory(requestFactory)
        .build();
  }
}
