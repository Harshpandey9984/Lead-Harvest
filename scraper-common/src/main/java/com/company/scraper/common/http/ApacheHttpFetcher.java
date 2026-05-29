package com.company.scraper.common.http;

import com.company.scraper.common.model.ProxyEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.classic.CloseableHttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

public class ApacheHttpFetcher implements HttpFetcher {

    private final CloseableHttpClient client;

    public ApacheHttpFetcher(CloseableHttpClient client) {
        this.client = client;
    }

    @Override
    public HttpResponse fetch(HttpRequest request, ProxyEndpoint proxy) {
        HttpUriRequestBase httpRequest = createRequest(request);
        if (request.headers() != null) {
            request.headers().forEach(httpRequest::addHeader);
        }
        if (request.body() != null && httpRequest instanceof HttpPost post) {
            post.setEntity(new StringEntity(request.body(), StandardCharsets.UTF_8));
        } else if (request.body() != null && httpRequest instanceof HttpPut put) {
            put.setEntity(new StringEntity(request.body(), StandardCharsets.UTF_8));
        } else if (request.body() != null && httpRequest instanceof HttpPatch patch) {
            patch.setEntity(new StringEntity(request.body(), StandardCharsets.UTF_8));
        }

        if (proxy != null) {
            RequestConfig config = RequestConfig.custom()
                .setProxy(new HttpHost(proxy.getProtocol(), proxy.getHost(), proxy.getPort()))
                .setConnectTimeout(Timeout.ofSeconds(10))
                .build();
            httpRequest.setConfig(config);
        }

        long start = System.currentTimeMillis();
        try (var response = client.execute(httpRequest)) {
            String payload = response.getEntity() == null
                ? ""
                : new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            long duration = System.currentTimeMillis() - start;
            return new HttpResponse(
                response.getCode(),
                payload,
                response.getHeaders().length == 0 ? Map.of() :
                    java.util.Arrays.stream(response.getHeaders())
                        .collect(Collectors.toMap(h -> h.getName(), h -> h.getValue(), (a, b) -> a + "," + b)),
                duration
            );
        } catch (IOException ex) {
            throw new IllegalStateException("HTTP request failed", ex);
        }
    }

    private HttpUriRequestBase createRequest(HttpRequest request) {
        return switch (request.method().toUpperCase()) {
            case "POST" -> new HttpPost(request.url());
            case "PUT" -> new HttpPut(request.url());
            case "PATCH" -> new HttpPatch(request.url());
            case "DELETE" -> new HttpDelete(request.url());
            default -> new HttpGet(request.url());
        };
    }
}
