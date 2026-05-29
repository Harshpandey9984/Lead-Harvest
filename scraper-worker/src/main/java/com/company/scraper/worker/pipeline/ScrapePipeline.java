package com.company.scraper.worker.pipeline;

import com.company.scraper.common.browser.BrowserFetcher;
import com.company.scraper.common.cache.DedupCache;
import com.company.scraper.common.cache.RedisRateLimiter;
import com.company.scraper.common.config.AppProperties;
import com.company.scraper.common.dto.ScrapeResultDto;
import com.company.scraper.common.dto.ScrapeTask;
import com.company.scraper.common.extractor.CssSelectorExtractor;
import com.company.scraper.common.extractor.JsonPointerExtractor;
import com.company.scraper.common.http.HttpFetcher;
import com.company.scraper.common.http.HttpRequest;
import com.company.scraper.common.http.HttpResponse;
import com.company.scraper.common.http.UserAgentRotator;
import com.company.scraper.common.model.ProxyEndpoint;
import com.company.scraper.common.monitoring.MetricsService;
import com.company.scraper.common.parser.JsoupParser;
import com.company.scraper.common.parser.JsonParser;
import com.company.scraper.common.pipeline.ChangeDetector;
import com.company.scraper.common.pipeline.ChangeDetectionResult;
import com.company.scraper.common.pipeline.DataNormalizer;
import com.company.scraper.common.pipeline.DataValidator;
import com.company.scraper.common.proxy.ProxyHealthChecker;
import com.company.scraper.common.proxy.ProxyRotationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class ScrapePipeline {

    private final HttpFetcher httpFetcher;
    private final BrowserFetcher browserFetcher;
    private final ProxyRotationService proxyRotationService;
    private final ProxyHealthChecker proxyHealthChecker;
    private final JsoupParser jsoupParser;
    private final JsonParser jsonParser;
    private final CssSelectorExtractor cssExtractor;
    private final JsonPointerExtractor jsonExtractor;
    private final DataNormalizer normalizer;
    private final DataValidator validator;
    private final ChangeDetector changeDetector;
    private final DedupCache dedupCache;
    private final MetricsService metricsService;
    private final UserAgentRotator userAgentRotator;
    private final RedisRateLimiter rateLimiter;
    private final AppProperties properties;

    public ScrapePipeline(HttpFetcher httpFetcher,
                          BrowserFetcher browserFetcher,
                          ProxyRotationService proxyRotationService,
                          ProxyHealthChecker proxyHealthChecker,
                          JsoupParser jsoupParser,
                          JsonParser jsonParser,
                          CssSelectorExtractor cssExtractor,
                          JsonPointerExtractor jsonExtractor,
                          DataNormalizer normalizer,
                          DataValidator validator,
                          ChangeDetector changeDetector,
                          DedupCache dedupCache,
                          MetricsService metricsService,
                          UserAgentRotator userAgentRotator,
                          RedisRateLimiter rateLimiter,
                          AppProperties properties) {
        this.httpFetcher = httpFetcher;
        this.browserFetcher = browserFetcher;
        this.proxyRotationService = proxyRotationService;
        this.proxyHealthChecker = proxyHealthChecker;
        this.jsoupParser = jsoupParser;
        this.jsonParser = jsonParser;
        this.cssExtractor = cssExtractor;
        this.jsonExtractor = jsonExtractor;
        this.normalizer = normalizer;
        this.validator = validator;
        this.changeDetector = changeDetector;
        this.dedupCache = dedupCache;
        this.metricsService = metricsService;
        this.userAgentRotator = userAgentRotator;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    public ScrapeResultDto process(ScrapeTask task, String previousHash) {
        ProxyEndpoint proxy = properties.getProxy().isEnabled() ? proxyRotationService.assign() : null;
        long start = System.currentTimeMillis();
        boolean success = false;
        try {
            applyRateLimit(task.url());
            applyHumanDelay();
            FetchOutcome outcome = switch (task.targetType()) {
                case STATIC -> extractHtml(task, proxy);
                case DYNAMIC -> extractDynamic(task, proxy);
                case API -> extractApi(task, proxy);
            };
            Map<String, Object> normalized = normalizer.normalize(outcome.payload());
            validator.validate(normalized);
            ChangeDetectionResult change = changeDetector.detect(previousHash, normalized);
            if (!change.changed()) {
                dedupCache.markIfNew("hash:" + change.newHash(), Duration.ofHours(12));
            }
            metricsService.recordSuccess(Duration.ofMillis(System.currentTimeMillis() - start));
            success = true;
            return new ScrapeResultDto(
                task.jobId(),
                task.targetId(),
                outcome.statusCode(),
                System.currentTimeMillis() - start,
                Instant.now(),
                change.changed(),
                normalized,
                change.newHash()
            );
        } catch (RuntimeException ex) {
            metricsService.recordFailure(Duration.ofMillis(System.currentTimeMillis() - start));
            throw ex;
        } finally {
            if (proxy != null) {
                if (success) {
                    proxyHealthChecker.markSuccess(proxy);
                } else {
                    proxyHealthChecker.markFailure(proxy);
                }
            }
        }
    }

    private FetchOutcome extractHtml(ScrapeTask task, ProxyEndpoint proxy) {
        HttpResponse response = httpFetcher.fetch(new HttpRequest(
            task.url(),
            task.method(),
            withDefaultHeaders(task.headers()),
            task.body(),
            Duration.ofSeconds(30)
        ), proxy);
        Document document = jsoupParser.parse(response.body(), task.url());
        return new FetchOutcome(cssExtractor.extract(document, task.selectors()), response.statusCode());
    }

    private FetchOutcome extractDynamic(ScrapeTask task, ProxyEndpoint proxy) {
        String html = browserFetcher.fetch(task, proxy);
        Document document = jsoupParser.parse(html, task.url());
        return new FetchOutcome(cssExtractor.extract(document, task.selectors()), 200);
    }

    private FetchOutcome extractApi(ScrapeTask task, ProxyEndpoint proxy) {
        HttpResponse response = httpFetcher.fetch(new HttpRequest(
            task.url(),
            task.method(),
            withDefaultHeaders(task.headers()),
            task.body(),
            Duration.ofSeconds(30)
        ), proxy);
        return new FetchOutcome(
            jsonExtractor.extract(jsonParser.parse(response.body()), task.selectors()),
            response.statusCode()
        );
    }

    private record FetchOutcome(Map<String, Object> payload, int statusCode) {
    }

    private Map<String, String> withDefaultHeaders(Map<String, String> headers) {
        Map<String, String> result = new java.util.HashMap<>();
        if (headers != null) {
            result.putAll(headers);
        }
        result.putIfAbsent("User-Agent", userAgentRotator.next());
        result.putIfAbsent("Accept", "text/html,application/json;q=0.9,*/*;q=0.8");
        result.putIfAbsent("Accept-Language", "en-US,en;q=0.8");
        return result;
    }

    private void applyRateLimit(String url) {
        String domain = java.net.URI.create(url).getHost();
        if (!rateLimiter.allow("domain:" + domain, 120, Duration.ofMinutes(1))) {
            throw new IllegalStateException("Rate limit exceeded for " + domain);
        }
    }

    private void applyHumanDelay() {
        long min = properties.getAntiBot().getMinDelay().toMillis();
        long max = properties.getAntiBot().getMaxDelay().toMillis();
        long delay = java.util.concurrent.ThreadLocalRandom.current().nextLong(min, Math.max(min + 1, max));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
