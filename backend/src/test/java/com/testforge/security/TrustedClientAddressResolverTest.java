package com.testforge.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.testforge.config.SecurityProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class TrustedClientAddressResolverTest {
  private final TrustedClientAddressResolver resolver =
      new TrustedClientAddressResolver(
          new SecurityProperties(List.of(), List.of("10.0.0.0/8"), 10, 10));

  /** Ignores spoofed forwarding data received directly from an untrusted peer. */
  @Test
  void ignoresForwardedAddressFromUntrustedPeer() {
    MockHttpServletRequest request = request("203.0.113.8", "198.51.100.4");

    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.8");
  }

  /** Accepts exactly one valid address supplied by a configured trusted proxy. */
  @Test
  void acceptsSingleForwardedAddressFromTrustedPeer() {
    MockHttpServletRequest request = request("10.2.3.4", "198.51.100.4");

    assertThat(resolver.resolve(request)).isEqualTo("198.51.100.4");
  }

  /** Falls back to the trusted socket peer for ambiguous or malformed forwarding data. */
  @Test
  void rejectsAmbiguousOrMalformedForwardedAddresses() {
    assertThat(resolver.resolve(request("10.2.3.4", "198.51.100.4, 203.0.113.8")))
        .isEqualTo("10.2.3.4");
    assertThat(resolver.resolve(request("10.2.3.4", "client.example.test"))).isEqualTo("10.2.3.4");
    assertThat(resolver.resolve(request("10.2.3.4", "198.51.100.4\r\nspoofed")))
        .isEqualTo("10.2.3.4");
    assertThat(resolver.resolve(request("10.2.3.4", "face.ca"))).isEqualTo("10.2.3.4");
    assertThat(resolver.resolve(request("10.2.3.4", "999.1.1.1"))).isEqualTo("10.2.3.4");
  }

  /** Accepts a numeric IPv6 client while rejecting a hostname-shaped hexadecimal token. */
  @Test
  void parsesIpv6WithoutHostnameResolution() {
    assertThat(resolver.resolve(request("10.2.3.4", "2001:db8::1")))
        .isEqualTo("2001:db8:0:0:0:0:0:1");
    assertThat(resolver.resolve(request("10.2.3.4", "::ffff:192.0.2.1"))).isEqualTo("192.0.2.1");
    assertThat(resolver.resolve(request("10.2.3.4", "192.0.2.1::"))).isEqualTo("10.2.3.4");
    assertThat(resolver.resolve(request("10.2.3.4", "192.0.2.1::1"))).isEqualTo("10.2.3.4");
    assertThat(resolver.resolve(request("10.2.3.4", "2001:db8:192.0.2.1::"))).isEqualTo("10.2.3.4");
    assertThat(resolver.resolve(request("10.2.3.4", "2001:db8:192.0.2.1:1"))).isEqualTo("10.2.3.4");
    assertThat(resolver.resolve(request("face.ca", null))).isEqualTo("invalid-peer");
  }

  /** Creates a servlet request with an explicit socket peer and optional forwarding header. */
  private MockHttpServletRequest request(String peer, String forwarded) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(peer);
    if (forwarded != null) request.addHeader("X-Forwarded-For", forwarded);
    return request;
  }
}
