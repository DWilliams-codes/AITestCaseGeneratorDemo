package com.testforge.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.testforge.common.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {
  @Test
  void limitsWithinAWindowAndResetsAtTheNextMinute() {
    MutableClock clock = new MutableClock(Instant.parse("2026-07-30T12:00:00Z"));
    RateLimitService limiter = new RateLimitService(clock);

    limiter.check("auth", "203.0.113.1", 1);
    assertThatThrownBy(() -> limiter.check("auth", "203.0.113.1", 1))
        .isInstanceOf(ApiException.class);

    clock.set(Instant.parse("2026-07-30T12:01:00Z"));
    limiter.check("auth", "203.0.113.1", 1);
  }

  @Test
  void evictsExpiredWindowsWhenTheDefensiveMapLimitIsReached() {
    MutableClock clock = new MutableClock(Instant.parse("2026-07-30T12:00:00Z"));
    RateLimitService limiter = new RateLimitService(clock);
    for (int index = 0; index <= 10_000; index++) {
      limiter.check("generation", "subject-" + index, 2);
    }
    clock.set(Instant.parse("2026-07-30T12:02:00Z"));
    limiter.check("generation", "current", 2);
  }

  private static final class MutableClock extends Clock {
    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    void set(Instant value) {
      instant = value;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
