package com.testforge.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "testforge.generation.provider", havingValue = "openai")
public class OpenAiConfiguration {
  /** Uses cancellable streams, a total response deadline, and no credential-bearing redirects. */
  @Bean
  @Qualifier("openAiRestClient") RestClient openAiRestClient(OpenAiProperties properties) {
    if (!StringUtils.hasText(properties.apiKey())) {
      throw new IllegalStateException(
          "OPENAI_API_KEY is required when TEST_GENERATION_PROVIDER=openai.");
    }
    HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .defaultHeader("Authorization", "Bearer " + properties.apiKey())
        .defaultHeader("Content-Type", "application/json")
        .requestFactory(factory)
        .build();
  }
}
