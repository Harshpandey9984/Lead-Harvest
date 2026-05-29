package com.company.scraper.common.http;

import java.time.Duration;
import java.util.Map;

public record HttpRequest(
    String url,
    String method,
    Map<String, String> headers,
    String body,
    Duration timeout
) { }
