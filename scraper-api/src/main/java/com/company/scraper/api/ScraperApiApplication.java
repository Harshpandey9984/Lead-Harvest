package com.company.scraper.api;

import com.company.scraper.common.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.company.scraper")
@EnableConfigurationProperties(AppProperties.class)
@EnableAsync
@EnableJpaRepositories(basePackages = "com.company.scraper")
@EntityScan(basePackages = "com.company.scraper")
public class ScraperApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScraperApiApplication.class, args);
    }
}
