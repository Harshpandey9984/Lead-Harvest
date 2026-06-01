package com.company.scraper.common.extractor;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class AddressExtractor {

    /**
     * Extracts a full address from a container element.
     *
     * Strategy:
     * - collect text lines from direct/semantic nodes (br, p, div, li)
     * - normalize whitespace and remove duplicate lines
     *
     * Returned value is a single string with lines joined by ", " (Excel-friendly).
     */
    public String extractAddress(Document document, String selector) {
        if (document == null || selector == null || selector.isBlank()) return null;

        Element container = document.selectFirst(selector);
        if (container == null) return null;

        List<String> lines = new ArrayList<>();

        // br-separated text
        String brSeparated = container.html() == null ? null : container.html();
        // Prefer explicit breaks
        for (Element br : container.select("br")) {
            // no-op: html scan below handles joining, but keeping this ensures br exists
        }

        // Prefer semantic block elements inside the container
        List<Element> blocks = container.select("p, div, li");
        if (!blocks.isEmpty()) {
            for (Element b : blocks) {
                String t = normalize(b.text());
                if (!t.isBlank()) lines.add(t);
            }
        } else {
            // Fallback: split by <br> via text and newline normalization
            String t = container.text();
            if (t != null) {
                String normalized = t.replace("\r\n", "\n").replace('\r', '\n');
                String[] parts = normalized.split("\n");
                for (String p : parts) {
                    String pt = normalize(p);
                    if (!pt.isBlank()) lines.add(pt);
                }
            }
        }

        lines = dedupPreserve(lines);
        if (lines.isEmpty()) return null;
        return String.join(", ", lines);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        // collapse whitespace
        return s.replaceAll("\\s+", " ").trim();
    }

    private static List<String> dedupPreserve(List<String> input) {
        List<String> out = new ArrayList<>();
        for (String s : input) {
            if (s == null || s.isBlank()) continue;
            boolean exists = false;
            for (String e : out) {
                if (e.equalsIgnoreCase(s)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) out.add(s);
        }
        return out;
    }
}

