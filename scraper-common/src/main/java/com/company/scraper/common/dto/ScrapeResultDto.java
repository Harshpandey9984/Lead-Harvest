package com.company.scraper.common.dto;

import java.time.Instant;
import java.util.Map;

public record ScrapeResultDto(
    Long jobId,
    Long targetId,
    int httpStatus,
    long durationMs,
    Instant fetchedAt,
    boolean changeDetected,
    Map<String, Object> payload,
    String contentHash
) { }
