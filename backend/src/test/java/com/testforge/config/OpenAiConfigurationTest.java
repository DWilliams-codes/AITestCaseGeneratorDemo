package com.testforge.config;

import static org.assertj.core.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OpenAiConfigurationTest {
  /** Proves cap and status rejection cancel a real streaming transport rather than draining it. */
  @Test
  void stopsOversizedAndRejectedStreamingResponses() throws Exception {
    for (int status : new int[] {200, 500}) {
      var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      var written = new AtomicInteger();
      var disconnected = new java.util.concurrent.CountDownLatch(1);
      final int attempted = 32 * 1024 * 1024;
      server.createContext(
          "/v1/responses",
          exchange -> {
            try {
              exchange.getRequestBody().readAllBytes();
              exchange.sendResponseHeaders(status, 0);
              byte[] chunk = new byte[8192];
              while (written.get() < attempted) {
                exchange.getResponseBody().write(chunk);
                exchange.getResponseBody().flush();
                written.addAndGet(chunk.length);
              }
            } catch (java.io.IOException expectedDisconnect) {
              disconnected.countDown();
            } finally {
              exchange.close();
            }
          });
      server.start();
      try {
        var properties =
            new OpenAiProperties(
                DemoModePolicy.FIXTURE_KEY,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                "fixture",
                1,
                3,
                100);
        var provider =
            new com.testforge.generation.provider.OpenAiTestGenerationProvider(
                properties,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new org.springframework.core.io.DefaultResourceLoader(),
                new OpenAiConfiguration().openAiRestClient(properties),
                org.mockito.Mockito.mock(DemoModePolicy.class));
        var request =
            new com.testforge.generation.provider.TestGenerationRequest(
                java.util.UUID.randomUUID(),
                "Synthetic",
                "Synthetic",
                "Synthetic",
                "Synthetic",
                java.util.List.of(
                    new com.testforge.generation.provider.TestGenerationRequest.CriterionInput(
                        "AC-1", "Synthetic result")),
                "correlation");
        assertThatThrownBy(() -> provider.generate(request))
            .isInstanceOf(com.testforge.generation.provider.GenerationProviderException.class)
            .hasMessageContaining(status == 200 ? "safe size limit" : "rejected");
        assertThat(disconnected.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(written.get()).isLessThan(attempted);
      } finally {
        server.stop(0);
      }
    }
  }

  /** Confirms the real configured transport cannot follow a redirect with its bearer header. */
  @Test
  void doesNotFollowRedirects() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var followed = new AtomicInteger();
    server.createContext(
        "/v1/responses",
        exchange -> {
          exchange.getResponseHeaders().add("Location", "/redirect-target");
          exchange.sendResponseHeaders(302, -1);
          exchange.close();
        });
    server.createContext(
        "/redirect-target",
        exchange -> {
          followed.incrementAndGet();
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    server.start();
    try {
      var client =
          new OpenAiConfiguration()
              .openAiRestClient(
                  new OpenAiProperties(
                      DemoModePolicy.FIXTURE_KEY,
                      "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                      "fixture",
                      1,
                      1,
                      100));
      var result =
          client
              .post()
              .uri("/responses")
              .body("{}")
              .exchange((request, response) -> response.getStatusCode().value());
      assertThat(result).isEqualTo(302);
      assertThat(followed.get()).isZero();
    } finally {
      server.stop(0);
    }
  }
}
