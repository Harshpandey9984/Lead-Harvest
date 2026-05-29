package com.company.scraper.common.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;

public class JsonPointerExtractor {

    public Map<String, Object> extract(JsonNode node, Map<String, String> pointers) {
        Map<String, Object> result = new HashMap<>();
        if (pointers == null) {
            return result;
        }
        pointers.forEach((key, pointer) -> {
            JsonNode value = node.at(pointer);
            if (value.isMissingNode() || value.isNull()) {
                result.put(key, null);
            } else if (value.isValueNode()) {
                result.put(key, value.asText());
            } else {
                result.put(key, value.toString());
            }
        });
        return result;
    }
}
