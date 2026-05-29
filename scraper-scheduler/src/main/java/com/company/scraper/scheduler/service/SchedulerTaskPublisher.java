package com.company.scraper.scheduler.service;

import com.company.scraper.common.config.AppProperties;
import com.company.scraper.common.dto.ScrapeTask;
import com.company.scraper.common.model.ScrapeTarget;
import com.company.scraper.common.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SchedulerTaskPublisher {

    private final KafkaTemplate<String, ScrapeTask> kafkaTemplate;
    private final AppProperties properties;
    private final ObjectMapper mapper;

    public SchedulerTaskPublisher(KafkaTemplate<String, ScrapeTask> kafkaTemplate,
                                  AppProperties properties,
                                  ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.mapper = mapper;
    }

    public void publish(ScrapeTarget target) {
        ScrapeTask task = new ScrapeTask(
            UUID.randomUUID().toString(),
            target.getJob().getId(),
            target.getId(),
            target.getUrl(),
            target.getMethod(),
            target.getTargetType(),
            toStringMap(target.getHeaders()),

            target.getBody(),
            toStringMap(target.getSelectors()),
            toMap(target.getPagination()),

            0,
            Instant.now()
        );
        kafkaTemplate.send(properties.getKafka().getScrapeTopic(), task.correlationId(), task);
    }

    private Map<String, String> toStringMap(String json) {
        if (json == null) {
            return null;
        }
        // JsonUtils#toMap returns Map<String, Object>, but ScrapeTask expects Map<String, String>
        Map<String, Object> raw = JsonUtils.toMap(mapper, json);
        return mapper.convertValue(raw, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
        });
    }



    private Map<String, Object> toMap(String json) {
        if (json == null) {
            return null;
        }
        return JsonUtils.toMap(mapper, json);
    }
}
