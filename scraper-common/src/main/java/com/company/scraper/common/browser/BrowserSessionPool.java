package com.company.scraper.common.browser;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;

public class BrowserSessionPool {

    private final BlockingQueue<WebDriver> pool;
    private final BrowserSessionFactory factory;
    private final int maxSize;

    public BrowserSessionPool(BrowserSessionFactory factory, int maxSize) {
        this.factory = factory;
        this.maxSize = maxSize;
        this.pool = new LinkedBlockingQueue<>();
    }

    public WebDriver acquire(Duration timeout) {
        try {
            WebDriver driver = pool.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (driver != null) {
                return driver;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return factory.create();
    }

    public void release(WebDriver driver) {
        if (driver == null) {
            return;
        }
        if (pool.size() >= maxSize) {
            driver.quit();
            return;
        }
        pool.offer(driver);
    }
}
