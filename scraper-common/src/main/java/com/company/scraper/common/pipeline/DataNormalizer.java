package com.company.scraper.common.pipeline;

import java.util.HashMap;
import java.util.Map;

public class DataNormalizer {

    public Map<String, Object> normalize(Map<String, Object> payload) {
        Map<String, Object> normalized = new HashMap<>();
        if (payload == null) {
            return normalized;
        }
        payload.forEach((key, value) -> {
            if (value instanceof String text) {
                normalized.put(key, text.trim());
            } else {
                normalized.put(key, value);
            }
        });
        return normalized;
    }
}
