package com.company.scraper.common.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class JsoupParser {

    public Document parse(String html, String baseUrl) {
        return Jsoup.parse(html, baseUrl);
    }
}
