package com.company.scraper.common.browser;

import com.company.scraper.common.dto.ScrapeTask;
import com.company.scraper.common.model.ProxyEndpoint;
import java.time.Duration;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class SeleniumBrowserFetcher implements BrowserFetcher {

    private final BrowserSessionPool pool;
    private final Duration acquireTimeout;

    public SeleniumBrowserFetcher(BrowserSessionPool pool, Duration acquireTimeout) {
        this.pool = pool;
        this.acquireTimeout = acquireTimeout;
    }

    @Override
    public String fetch(ScrapeTask task, ProxyEndpoint proxy) {
        WebDriver driver = null;
        boolean pooled = proxy == null;
        if (pooled) {
            driver = pool.acquire(acquireTimeout);
        } else {
            ChromeOptions options = new ChromeOptions();
            Proxy seleniumProxy = new Proxy();
            seleniumProxy.setHttpProxy(proxy.getHost() + ":" + proxy.getPort());
            options.setProxy(seleniumProxy);
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            driver = new org.openqa.selenium.chrome.ChromeDriver(options);
        }
        try {
            driver.get(task.url());
            if (driver instanceof JavascriptExecutor executor) {
                executor.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            }
            return driver.getPageSource();
        } finally {
            if (pooled) {
                pool.release(driver);
            } else if (driver != null) {
                driver.quit();
            }
        }
    }
}
