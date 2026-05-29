package com.company.scraper.common.browser;

import com.company.scraper.common.dto.ScrapeTask;
import com.company.scraper.common.model.ProxyEndpoint;

public interface BrowserFetcher {
    String fetch(ScrapeTask task, ProxyEndpoint proxy);
}
