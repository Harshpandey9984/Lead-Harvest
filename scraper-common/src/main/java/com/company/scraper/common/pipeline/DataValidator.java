package com.company.scraper.common.pipeline;

import java.util.Map;

public class DataValidator {

    public void validate(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Empty payload");
        }
    }
}
