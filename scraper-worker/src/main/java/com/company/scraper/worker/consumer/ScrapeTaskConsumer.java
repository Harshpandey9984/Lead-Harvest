package com.company.scraper.worker.consumer;

import com.company.scraper.common.concurrency.WorkerExecutor;
import com.company.scraper.common.dto.ScrapeTask;
import com.company.scraper.worker.service.ScrapeProcessorService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ScrapeTaskConsumer {

    private final ScrapeProcessorService processorService;
    private final WorkerExecutor executor;
    public ScrapeTaskConsumer(ScrapeProcessorService processorService,
                              WorkerExecutor executor) {
        this.processorService = processorService;
        this.executor = executor;
    }

    @KafkaListener(topics = {"${scraper.kafka.scrape-topic}", "${scraper.kafka.retry-topic}"})
    public void onMessage(ScrapeTask task) {
        executor.submit(() -> {
            processorService.process(task);
            return null;
        });
    }
}
