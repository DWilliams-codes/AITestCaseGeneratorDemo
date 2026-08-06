package com.testforge.security;

import com.testforge.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

/** Resolves one client address only across an explicitly configured proxy trust boundary. */
@Component
public class TrustedClientAddressResolver {
  private static final String FORWARDED_FOR = "X-Forwarded-For";
  private final List<IpAddressMatcher> trustedProxies;

  /** Initializes TrustedClientAddressResolver with its required collaborators and domain state. */
  public TrustedClientAddressResolver(SecurityProperties properties) {
    this.trustedProxies =
        properties.trustedProxyCidrs().stream().map(IpAddressMatcher::new).toList();
  }

  /** Ignores caller-supplied forwarding data unless the socket peer is trusted. */
  public String resolve(HttpServletRequest request) {
    String peer = normalizeLiteral(request.getRemoteAddr());
    if (peer == null) {
      return "invalid-peer";
    }
    if (trustedProxies.stream().noneMatch(matcher -> matcher.matches(peer))) {
      return peer;
    }
    String forwarded = request.getHeader(FORWARDED_FOR);
    if (forwarded == null || forwarded.contains(",")) {
      return peer;
    }
    String normalizedForwarded = normalizeLiteral(forwarded.strip());
    return normalizedForwarded == null ? peer : normalizedForwarded;
  }

  /** Parses only numeric IPv4/IPv6 literals so address resolution never performs DNS. */
  private String normalizeLiteral(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    byte[] address = value.indexOf(':') >= 0 ? parseIpv6(value) : parseIpv4(value);
    if (address == null) return null;
    try {
      return InetAddress.getByAddress(address).getHostAddress();
    } catch (UnknownHostException exception) {
      return null;
    }
  }

  /** Parses four decimal octets without invoking a hostname resolver. */
  private byte[] parseIpv4(String value) {
    String[] octets = value.split("\\.", -1);
    if (octets.length != 4) return null;
    byte[] address = new byte[4];
    for (int index = 0; index < octets.length; index++) {
      String octet = octets[index];
      if (octet.isEmpty() || octet.length() > 3 || !octet.chars().allMatch(Character::isDigit)) {
        return null;
      }
      int numeric = Integer.parseInt(octet);
      if (numeric > 255) return null;
      address[index] = (byte) numeric;
    }
    return address;
  }

  /** Parses RFC-style hexadecimal groups, including one compressed run and an IPv4 tail. */
  private byte[] parseIpv6(String value) {
    if (value.contains("%") || value.indexOf("::") != value.lastIndexOf("::")) return null;
    boolean compressed = value.contains("::");
    String[] halves = value.split("::", -1);
    if ((!compressed && halves.length != 1) || (compressed && halves.length != 2)) return null;
    var left = parseIpv6Groups(halves[0], !compressed);
    var right = compressed ? parseIpv6Groups(halves[1], true) : new ArrayList<Integer>();
    if (left == null || right == null) return null;
    int supplied = left.size() + right.size();
    if ((!compressed && supplied != 8) || (compressed && supplied >= 8)) return null;
    int zeros = 8 - supplied;
    byte[] address = new byte[16];
    int offset = 0;
    for (int group : left) offset = writeGroup(address, offset, group);
    offset += zeros * 2;
    for (int group : right) offset = writeGroup(address, offset, group);
    return offset == 16 ? address : null;
  }

  /** Converts one colon-delimited side into 16-bit groups without DNS or lenient parsing. */
  private ArrayList<Integer> parseIpv6Groups(String value, boolean allowIpv4Tail) {
    var groups = new ArrayList<Integer>();
    if (value.isEmpty()) return groups;
    String[] parts = value.split(":", -1);
    for (int index = 0; index < parts.length; index++) {
      String part = parts[index];
      if (part.isEmpty()) return null;
      if (part.indexOf('.') >= 0) {
        if (!allowIpv4Tail || index != parts.length - 1) return null;
        byte[] ipv4 = parseIpv4(part);
        if (ipv4 == null) return null;
        groups.add(((ipv4[0] & 0xff) << 8) | (ipv4[1] & 0xff));
        groups.add(((ipv4[2] & 0xff) << 8) | (ipv4[3] & 0xff));
      } else {
        if (part.length() > 4 || !part.matches("[0-9A-Fa-f]+")) return null;
        groups.add(Integer.parseInt(part, 16));
      }
    }
    return groups;
  }

  /** Writes one validated 16-bit group to its network-order address slot. */
  private int writeGroup(byte[] address, int offset, int group) {
    address[offset] = (byte) (group >>> 8);
    address[offset + 1] = (byte) group;
    return offset + 2;
  }
}
