package com.company.scraper.common.dto;

import com.company.scraper.common.model.TargetType;
import java.time.Instant;
import java.util.Map;

public record ScrapeTask(
    String correlationId,
    Long jobId,
    Long targetId,
    String url,
    String method,
    TargetType targetType,
    Map<String, String> headers,
    String body,
    Map<String, String> selectors,
    Map<String, Object> pagination,
    int attempt,
    Instant createdAt
) { }
