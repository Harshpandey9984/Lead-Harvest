package com.company.scraper.common.http;

import java.util.Map;

public record HttpResponse(
    int statusCode,
    String body,
    Map<String, String> headers,
    long durationMs
) { }
