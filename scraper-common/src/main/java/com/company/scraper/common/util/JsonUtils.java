package com.company.scraper.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static String toJson(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize payload", ex);
        }
    }

    public static Map<String, Object> toMap(ObjectMapper mapper, String json) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize payload", ex);
        }
    }
}
