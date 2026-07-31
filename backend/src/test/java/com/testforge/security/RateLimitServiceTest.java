package com.testforge.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.testforge.common.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {
  /** Covers the limits within a window and resets at the next minute scenario. */
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

  /** Covers the evicts expired windows when the defensive map limit is reached scenario. */
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

    /** Initializes MutableClock with its required collaborators and domain state. */
    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    /** Executes the set operation for MutableClock. */
    void set(Instant value) {
      instant = value;
    }

    /** Returns the current zone value. */
    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    /** Executes the with zone operation for MutableClock. */
    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    /** Executes the instant operation for MutableClock. */
    @Override
    public Instant instant() {
      return instant;
    }
  }
}
