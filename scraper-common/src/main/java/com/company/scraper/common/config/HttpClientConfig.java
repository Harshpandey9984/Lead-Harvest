package com.company.scraper.common.config;

import com.company.scraper.common.http.UserAgentRotator;
import java.time.Duration;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

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
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .writeTimeout(writeTimeout)
            .connectionPool(new ConnectionPool(
                properties.getHttp().getMaxTotalConnections(),
                5,
                java.util.concurrent.TimeUnit.MINUTES));
        
        return configureTrustAll(builder).build();
    }

    private static OkHttpClient.Builder configureTrustAll(OkHttpClient.Builder builder) {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                new javax.net.ssl.X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            // ignore
        }
        return builder;
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
