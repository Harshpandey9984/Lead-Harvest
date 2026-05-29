package com.company.scraper.worker.config;

import com.company.scraper.common.browser.BrowserFetcher;
import com.company.scraper.common.browser.BrowserSessionPool;
import com.company.scraper.common.browser.ChromeSessionFactory;
import com.company.scraper.common.browser.SeleniumBrowserFetcher;
import com.company.scraper.common.config.AppProperties;
import com.company.scraper.common.extractor.CssSelectorExtractor;
import com.company.scraper.common.extractor.JsonPointerExtractor;
import com.company.scraper.common.http.HttpFetcher;
import com.company.scraper.common.http.OkHttpFetcher;
import com.company.scraper.common.parser.JsoupParser;
import com.company.scraper.common.parser.JsonParser;
import com.company.scraper.common.pipeline.ChangeDetector;
import com.company.scraper.common.pipeline.DataNormalizer;
import com.company.scraper.common.pipeline.DataValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PipelineConfig {

    @Bean
    public HttpFetcher httpFetcher(OkHttpClient okHttpClient) {
        return new OkHttpFetcher(okHttpClient);
    }

    @Bean
    public BrowserFetcher browserFetcher(AppProperties properties) {
        ChromeSessionFactory factory = new ChromeSessionFactory(List.of("--disable-blink-features=AutomationControlled"));
        BrowserSessionPool pool = new BrowserSessionPool(factory, properties.getConcurrency().getBrowserThreads());
        return new SeleniumBrowserFetcher(pool, Duration.ofSeconds(10));
    }

    @Bean
    public JsoupParser jsoupParser() {
        return new JsoupParser();
    }

    @Bean
    public JsonParser jsonParser(ObjectMapper mapper) {
        return new JsonParser(mapper);
    }

    @Bean
    public CssSelectorExtractor cssSelectorExtractor() {
        return new CssSelectorExtractor();
    }

    @Bean
    public JsonPointerExtractor jsonPointerExtractor() {
        return new JsonPointerExtractor();
    }

    @Bean
    public DataNormalizer dataNormalizer() {
        return new DataNormalizer();
    }

    @Bean
    public DataValidator dataValidator() {
        return new DataValidator();
    }

    @Bean
    public ChangeDetector changeDetector(ObjectMapper mapper) {
        return new ChangeDetector(mapper);
    }
}
