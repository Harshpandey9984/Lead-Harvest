package com.company.scraper.common.http;

import com.company.scraper.common.model.ProxyEndpoint;

public interface HttpFetcher {
    HttpResponse fetch(HttpRequest request, ProxyEndpoint proxy);
}
