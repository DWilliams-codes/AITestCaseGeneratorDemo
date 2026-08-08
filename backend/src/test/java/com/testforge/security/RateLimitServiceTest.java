package com.testforge.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.testforge.common.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
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
    for (int index = 0; index < RateLimitService.MAX_ACTIVE_WINDOWS; index++) {
      limiter.check("generation", "subject-" + index, 2);
    }
    assertThatThrownBy(() -> limiter.check("generation", "overflow", 2))
        .isInstanceOf(ApiException.class);
    assertThat(limiter.trackedWindowCount()).isEqualTo(RateLimitService.MAX_ACTIVE_WINDOWS);
    for (int index = 0; index < 1_000; index++) {
      assertThatThrownBy(() -> limiter.check("generation", "overflow", 2))
          .isInstanceOf(ApiException.class);
    }
    assertThat(limiter.trackedWindowCount()).isEqualTo(RateLimitService.MAX_ACTIVE_WINDOWS);
    clock.set(Instant.parse("2026-07-30T12:02:00Z"));
    limiter.check("generation", "current", 2);
    assertThat(limiter.trackedWindowCount()).isEqualTo(1);
  }

  /** Proves concurrent distinct-subject admission cannot exceed the current-minute hard cap. */
  @Test
  void boundsConcurrentDistinctSubjects() {
    RateLimitService limiter =
        new RateLimitService(Clock.fixed(Instant.parse("2026-07-30T12:00:00Z"), ZoneOffset.UTC));
    ConcurrentLinkedQueue<ApiException> rejected = new ConcurrentLinkedQueue<>();

    IntStream.range(0, RateLimitService.MAX_ACTIVE_WINDOWS + 256)
        .parallel()
        .forEach(
            index -> {
              try {
                limiter.check("auth", "subject-" + index, 2);
              } catch (ApiException exception) {
                rejected.add(exception);
              }
            });

    assertThat(limiter.trackedWindowCount()).isEqualTo(RateLimitService.MAX_ACTIVE_WINDOWS);
    assertThat(rejected).hasSize(256);
  }

  /** Prevents a delayed old-minute request from rolling the active window backward. */
  @Test
  void staleMinuteThreadCannotClearCurrentCountsOrRestoreAdmission() throws Exception {
    Instant oldMinute = Instant.parse("2026-07-30T12:00:00Z");
    Instant currentMinute = Instant.parse("2026-07-30T12:01:00Z");
    RateLimitService limiter =
        new RateLimitService(new ThreadMinuteClock(oldMinute, currentMinute, "stale-minute"));
    CountDownLatch staleStarted = new CountDownLatch(1);
    CountDownLatch resumeStale = new CountDownLatch(1);
    ExecutorService executor =
        Executors.newSingleThreadExecutor(task -> new Thread(task, "stale-minute"));
    try {
      Future<Void> staleRequest =
          executor.submit(
              () -> {
                staleStarted.countDown();
                resumeStale.await();
                limiter.check("auth", "stale-client", 1);
                return null;
              });
      staleStarted.await();
      limiter.check("auth", "current-client", 1);
      resumeStale.countDown();
      staleRequest.get();

      assertThat(limiter.trackedWindowCount()).isEqualTo(2);
      assertThatThrownBy(() -> limiter.check("auth", "current-client", 1))
          .isInstanceOf(ApiException.class);
    } finally {
      resumeStale.countDown();
      executor.shutdownNow();
    }
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

  /** Returns a deliberately stale minute only to the named delayed request thread. */
  private static final class ThreadMinuteClock extends Clock {
    private final Instant staleMinute;
    private final Instant currentMinute;
    private final String staleThreadName;

    /** Initializes the controlled clock with per-thread minute observations. */
    private ThreadMinuteClock(Instant staleMinute, Instant currentMinute, String staleThreadName) {
      this.staleMinute = staleMinute;
      this.currentMinute = currentMinute;
      this.staleThreadName = staleThreadName;
    }

    /** Uses UTC because the limiter groups only absolute instants. */
    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    /** Retains the fixed per-thread instants for any requested zone. */
    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    /** Returns the stale minute only to the delayed request thread. */
    @Override
    public Instant instant() {
      return staleThreadName.equals(Thread.currentThread().getName()) ? staleMinute : currentMinute;
    }
  }
}
