package com.company.scraper.common.extractor;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CssSelectorExtractor {

    private final SmartLinkExtractor smartLinkExtractor = new SmartLinkExtractor();
    private final AddressExtractor addressExtractor = new AddressExtractor();

    /**
     * Selector syntax (backward compatible):
     * - "css selector" => extract element.text() (existing behavior)
     * - "href@css(<selector>)" => extract href from matching elements; multiple values are joined with ", "
     * - "tel@css(<selector>)" => extract phone number from tel: links (or best-effort from href)
     * - "mailto@css(<selector>)" => extract email from mailto: links (or best-effort)
     */
    public Map<String, Object> extract(Document document, Map<String, String> selectors) {
        Map<String, Object> result = new HashMap<>();
        if (selectors == null) {
            return result;
        }

        selectors.forEach((key, selectorSpec) -> {
            if (selectorSpec == null || selectorSpec.isBlank()) {
                result.put(key, null);
                return;
            }

            String spec = selectorSpec.trim();
            if (spec.startsWith("href@css(") && spec.endsWith(")")) {
                String inner = spec.substring("href@css(".length(), spec.length() - 1);
                result.put(key, smartLinkExtractor.extract(document, inner, SmartLinkExtractor.LinkType.HREF));
                return;
            }
            if (spec.startsWith("tel@css(") && spec.endsWith(")")) {
                String inner = spec.substring("tel@css(".length(), spec.length() - 1);
                result.put(key, smartLinkExtractor.extract(document, inner, SmartLinkExtractor.LinkType.TEL));
                return;
            }
            if (spec.startsWith("mailto@css(") && spec.endsWith(")")) {
                String inner = spec.substring("mailto@css(".length(), spec.length() - 1);
                result.put(key, smartLinkExtractor.extract(document, inner, SmartLinkExtractor.LinkType.MAILTO));
                return;
            }

            // address container extraction (joins semantic child nodes)
            if (spec.startsWith("address@css(") && spec.endsWith(")")) {
                String inner = spec.substring("address@css(".length(), spec.length() - 1);
                result.put(key, addressExtractor.extractAddress(document, inner));
                return;
            }


            // legacy behavior: first matched element's text
            Element element = document.selectFirst(spec);
            result.put(key, element == null ? null : element.text());
        });
        return result;
    }
}

