package com.testforge.security;

import com.testforge.common.error.ApiExceptions;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
  private final Clock clock;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  /** Initializes RateLimitService with its required collaborators and domain state. */
  public RateLimitService(Clock clock) {
    this.clock = clock;
  }

  /** Executes the check operation for RateLimitService. */
  public void check(String bucket, String subject, int limit) {
    Instant minute = clock.instant().truncatedTo(ChronoUnit.MINUTES);
    String key = bucket + ':' + subject;
    Window current = windows.compute(key, (ignored, prior) -> nextWindow(prior, minute));
    if (current.count() > limit) {
      throw ApiExceptions.tooManyRequests("Too many requests. Try again in one minute.");
    }
    if (windows.size() > 10_000) {
      windows.entrySet().removeIf(entry -> entry.getValue().minute().isBefore(minute));
    }
  }

  /** Executes the next window operation for RateLimitService. */
  private Window nextWindow(Window prior, Instant minute) {
    if (prior == null || !prior.minute().equals(minute)) {
      return new Window(minute, 1);
    }
    return new Window(minute, prior.count() + 1);
  }

  private record Window(Instant minute, int count) {}
}
