package com.company.scraper.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "scraper")
public class AppProperties {

    private final Concurrency concurrency = new Concurrency();
    private final Http http = new Http();
    private final Proxy proxy = new Proxy();
    private final AntiBot antiBot = new AntiBot();
    private final Kafka kafka = new Kafka();
    private final Notification notification = new Notification();

    public Concurrency getConcurrency() {
        return concurrency;
    }

    public Http getHttp() {
        return http;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public AntiBot getAntiBot() {
        return antiBot;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public Notification getNotification() {
        return notification;
    }

    public static class Concurrency {
        @Min(1)
        private int workerThreads = 32;
        @Min(1)
        private int browserThreads = 8;
        @Min(1)
        private int maxQueueDepth = 50000;

        public int getWorkerThreads() {
            return workerThreads;
        }

        public void setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
        }

        public int getBrowserThreads() {
            return browserThreads;
        }

        public void setBrowserThreads(int browserThreads) {
            this.browserThreads = browserThreads;
        }

        public int getMaxQueueDepth() {
            return maxQueueDepth;
        }

        public void setMaxQueueDepth(int maxQueueDepth) {
            this.maxQueueDepth = maxQueueDepth;
        }
    }

    public static class Http {
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration writeTimeout = Duration.ofSeconds(15);
        @Min(1)
        @Max(10_000)
        private int maxTotalConnections = 1000;
        @Min(1)
        @Max(10_000)
        private int maxPerRoute = 200;
        private boolean http2Enabled = true;

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Duration getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(Duration writeTimeout) {
            this.writeTimeout = writeTimeout;
        }

        public int getMaxTotalConnections() {
            return maxTotalConnections;
        }

        public void setMaxTotalConnections(int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
        }

        public int getMaxPerRoute() {
            return maxPerRoute;
        }

        public void setMaxPerRoute(int maxPerRoute) {
            this.maxPerRoute = maxPerRoute;
        }

        public boolean isHttp2Enabled() {
            return http2Enabled;
        }

        public void setHttp2Enabled(boolean http2Enabled) {
            this.http2Enabled = http2Enabled;
        }
    }

    public static class Proxy {
        private boolean enabled = true;
        private Duration healthCheckInterval = Duration.ofMinutes(3);
        @Min(0)
        @Max(100)
        private int banThreshold = 20;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getHealthCheckInterval() {
            return healthCheckInterval;
        }

        public void setHealthCheckInterval(Duration healthCheckInterval) {
            this.healthCheckInterval = healthCheckInterval;
        }

        public int getBanThreshold() {
            return banThreshold;
        }

        public void setBanThreshold(int banThreshold) {
            this.banThreshold = banThreshold;
        }
    }

    public static class AntiBot {
        private Duration minDelay = Duration.ofMillis(250);
        private Duration maxDelay = Duration.ofSeconds(3);
        private boolean useStealthHeaders = true;

        public Duration getMinDelay() {
            return minDelay;
        }

        public void setMinDelay(Duration minDelay) {
            this.minDelay = minDelay;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
        }

        public boolean isUseStealthHeaders() {
            return useStealthHeaders;
        }

        public void setUseStealthHeaders(boolean useStealthHeaders) {
            this.useStealthHeaders = useStealthHeaders;
        }
    }

    public static class Kafka {
        @NotBlank
        private String scrapeTopic = "scrape.tasks";
        @NotBlank
        private String retryTopic = "scrape.retry";
        @NotBlank
        private String dlqTopic = "scrape.dlq";

        public String getScrapeTopic() {
            return scrapeTopic;
        }

        public void setScrapeTopic(String scrapeTopic) {
            this.scrapeTopic = scrapeTopic;
        }

        public String getRetryTopic() {
            return retryTopic;
        }

        public void setRetryTopic(String retryTopic) {
            this.retryTopic = retryTopic;
        }

        public String getDlqTopic() {
            return dlqTopic;
        }

        public void setDlqTopic(String dlqTopic) {
            this.dlqTopic = dlqTopic;
        }
    }

    public static class Notification {
        private boolean enabled = true;
        private String telegramBotToken;
        private String telegramChatId;
        private String webhookUrl;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTelegramBotToken() {
            return telegramBotToken;
        }

        public void setTelegramBotToken(String telegramBotToken) {
            this.telegramBotToken = telegramBotToken;
        }

        public String getTelegramChatId() {
            return telegramChatId;
        }

        public void setTelegramChatId(String telegramChatId) {
            this.telegramChatId = telegramChatId;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
    }
}
