package com.company.scraper.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer latencyTimer;

    public MetricsService(MeterRegistry registry) {
        this.successCounter = registry.counter("scraper.requests.success");
        this.failureCounter = registry.counter("scraper.requests.failure");
        this.latencyTimer = registry.timer("scraper.requests.latency");
    }

    public void recordSuccess(Duration duration) {
        successCounter.increment();
        latencyTimer.record(duration);
    }

    public void recordFailure(Duration duration) {
        failureCounter.increment();
        latencyTimer.record(duration);
    }
}
