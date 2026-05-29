package com.company.scraper.scheduler;

import com.company.scraper.common.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.company.scraper")
@EnableConfigurationProperties(AppProperties.class)
public class ScraperSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScraperSchedulerApplication.class, args);
    }
}
