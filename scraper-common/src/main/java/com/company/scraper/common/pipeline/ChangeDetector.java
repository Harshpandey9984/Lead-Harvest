package com.company.scraper.common.pipeline;

import com.company.scraper.common.util.HashUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class ChangeDetector {

    private final ObjectMapper mapper;

    public ChangeDetector(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ChangeDetectionResult detect(String previousHash, Map<String, Object> payload) {
        try {
            String serialized = mapper.writeValueAsString(payload);
            String newHash = HashUtils.sha256(serialized);
            boolean changed = previousHash == null || !previousHash.equals(newHash);
            return new ChangeDetectionResult(changed, newHash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute change hash", ex);
        }
    }
}
