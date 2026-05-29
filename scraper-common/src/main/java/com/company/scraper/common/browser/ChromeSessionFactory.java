package com.company.scraper.common.browser;

import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeSessionFactory implements BrowserSessionFactory {

    private final List<String> baseArguments;

    public ChromeSessionFactory(List<String> baseArguments) {
        this.baseArguments = baseArguments;
    }

    @Override
    public WebDriver create() {
        ChromeOptions options = new ChromeOptions();
        baseArguments.forEach(options::addArguments);
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        return new ChromeDriver(options);
    }
}
