package com.company.scraper.common.retry;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class RetryExecutor {

    private final RetryPolicy policy;

    public RetryExecutor(RetryPolicy policy) {
        this.policy = policy;
    }

    public <T> T execute(Supplier<T> action) {
        int attempt = 0;
        RuntimeException last = null;
        while (attempt < policy.maxAttempts()) {
            attempt++;
            try {
                return action.get();
            } catch (RuntimeException ex) {
                last = ex;
                sleep(backoff(attempt));
            }
        }
        throw last == null ? new IllegalStateException("Retry failed") : last;
    }

    private Duration backoff(int attempt) {
        long base = policy.baseDelay().toMillis();
        long max = policy.maxDelay().toMillis();
        long delay = Math.min(max, base * attempt);
        long jitter = ThreadLocalRandom.current().nextLong(50, 150);
        return Duration.ofMillis(delay + jitter);
    }

    private void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
