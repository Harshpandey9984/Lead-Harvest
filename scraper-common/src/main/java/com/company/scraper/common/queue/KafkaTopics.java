package com.company.scraper.common.queue;

public final class KafkaTopics {

    public static final String SCRAPE_TASKS = "scrape.tasks";
    public static final String SCRAPE_RETRY = "scrape.retry";
    public static final String SCRAPE_DLQ = "scrape.dlq";

    private KafkaTopics() {
    }
}
