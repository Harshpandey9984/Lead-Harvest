package com.company.scraper.common.retry;

import java.time.Duration;

public record RetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay) {
}
