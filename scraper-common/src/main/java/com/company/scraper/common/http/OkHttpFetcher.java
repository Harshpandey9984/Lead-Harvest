package com.company.scraper.common.http;

import com.company.scraper.common.model.ProxyEndpoint;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpFetcher implements HttpFetcher {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient client;

    public OkHttpFetcher(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public HttpResponse fetch(HttpRequest request, ProxyEndpoint proxy) {
        OkHttpClient effectiveClient = proxy == null ? client : client.newBuilder()
            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())))
            .build();

        Request.Builder builder = new Request.Builder().url(request.url());
        if (request.headers() != null) {
            request.headers().forEach(builder::addHeader);
        }

        if (!"GET".equalsIgnoreCase(request.method())) {
            RequestBody body = RequestBody.create(request.body() == null ? "" : request.body(), JSON);
            builder.method(request.method(), body);
        }

        long start = System.currentTimeMillis();
        try (Response response = effectiveClient.newCall(builder.build()).execute()) {
            ResponseBody body = response.body();
            String payload = body == null ? "" : body.string();
            long duration = System.currentTimeMillis() - start;
            return new HttpResponse(response.code(), payload, flattenHeaders(response), duration);
        } catch (IOException ex) {
            throw new IllegalStateException("HTTP request failed", ex);
        }
    }

    private Map<String, String> flattenHeaders(Response response) {
        return response.headers().toMultimap().entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> String.join(",", entry.getValue())));
    }
}
