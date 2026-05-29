package com.company.scraper.common.extractor;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CssSelectorExtractor {

    public Map<String, Object> extract(Document document, Map<String, String> selectors) {
        Map<String, Object> result = new HashMap<>();
        if (selectors == null) {
            return result;
        }
        selectors.forEach((key, selector) -> {
            Element element = document.selectFirst(selector);
            result.put(key, element == null ? null : element.text());
        });
        return result;
    }
}
