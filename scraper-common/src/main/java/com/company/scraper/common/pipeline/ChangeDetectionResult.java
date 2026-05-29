package com.company.scraper.common.pipeline;

public record ChangeDetectionResult(
    boolean changed,
    String newHash
) { }
