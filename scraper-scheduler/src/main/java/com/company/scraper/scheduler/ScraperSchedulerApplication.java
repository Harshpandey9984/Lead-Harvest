package com.company.scraper.scheduler;

import com.company.scraper.common.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.company.scraper")
@EnableJpaRepositories(basePackages = "com.company.scraper.common.repository")
@EntityScan(basePackages = "com.company.scraper.common.model")
@EnableConfigurationProperties(AppProperties.class)
public class ScraperSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScraperSchedulerApplication.class, args);
    }
}
