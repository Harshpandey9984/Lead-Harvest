package com.company.scraper.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    @Bean(name = "scrapeExecutor")
    public Executor scrapeExecutor(AppProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getConcurrency().getWorkerThreads());
        executor.setMaxPoolSize(properties.getConcurrency().getWorkerThreads());
        executor.setQueueCapacity(properties.getConcurrency().getMaxQueueDepth());
        executor.setThreadNamePrefix("scrape-worker-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "browserExecutor")
    public Executor browserExecutor(AppProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getConcurrency().getBrowserThreads());
        executor.setMaxPoolSize(properties.getConcurrency().getBrowserThreads());
        executor.setQueueCapacity(properties.getConcurrency().getMaxQueueDepth());
        executor.setThreadNamePrefix("browser-worker-");
        executor.initialize();
        return executor;
    }
}
