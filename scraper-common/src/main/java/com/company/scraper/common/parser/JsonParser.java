package com.company.scraper.common.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonParser {

    private final ObjectMapper mapper;

    public JsonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }
}
