package com.company.scraper.common.browser;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;

public class BrowserSessionPool {

    private final BlockingQueue<WebDriver> pool;
    private final BrowserSessionFactory factory;
    private final int maxSize;
    private final Semaphore semaphore;

    public BrowserSessionPool(BrowserSessionFactory factory, int maxSize) {
        this.factory = factory;
        this.maxSize = maxSize;
        this.pool = new LinkedBlockingQueue<>();
        this.semaphore = new Semaphore(maxSize);
    }

    public WebDriver acquire(Duration timeout) {
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (!acquired) {
            throw new IllegalStateException("Timeout waiting to acquire browser session");
        }

        WebDriver driver = pool.poll();
        if (driver != null) {
            return driver;
        }
        try {
            return factory.create();
        } catch (Exception ex) {
            semaphore.release();
            throw ex;
        }
    }

    public void release(WebDriver driver) {
        if (driver == null) {
            return;
        }
        try {
            if (pool.offer(driver)) {
                // Return driver to pool
            } else {
                driver.quit();
            }
        } finally {
            semaphore.release();
        }
    }
}
