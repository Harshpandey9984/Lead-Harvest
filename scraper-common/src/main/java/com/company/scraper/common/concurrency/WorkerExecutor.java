package com.company.scraper.common.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class WorkerExecutor {

    private final Executor executor;

    public WorkerExecutor(@Qualifier("scrapeExecutor") Executor scrapeExecutor) {
        this.executor = scrapeExecutor;
    }

    public <T> CompletableFuture<T> submit(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }
}
