package com.company.scraper.common.config;

import com.company.scraper.common.http.UserAgentRotator;
import java.time.Duration;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.hc.client5.http.classic.CloseableHttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient(AppProperties properties) {
        Duration connectTimeout = properties.getHttp().getConnectTimeout();
        Duration readTimeout = properties.getHttp().getReadTimeout();
        Duration writeTimeout = properties.getHttp().getWriteTimeout();
        return new OkHttpClient.Builder()
            .connectTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .writeTimeout(writeTimeout)
            .connectionPool(new ConnectionPool(
                properties.getHttp().getMaxTotalConnections(),
                5,
                java.util.concurrent.TimeUnit.MINUTES))
            .build();
    }

    @Bean
    public CloseableHttpClient apacheHttpClient(AppProperties properties) {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(properties.getHttp().getMaxTotalConnections());
        manager.setDefaultMaxPerRoute(properties.getHttp().getMaxPerRoute());
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(Timeout.of(properties.getHttp().getConnectTimeout()))
            .setResponseTimeout(Timeout.of(properties.getHttp().getReadTimeout()))
            .build();
        return HttpClients.custom()
            .setDefaultRequestConfig(config)
            .setConnectionManager(manager)
            .build();
    }

    @Bean
    public UserAgentRotator userAgentRotator() {
        return new UserAgentRotator();
    }
}
