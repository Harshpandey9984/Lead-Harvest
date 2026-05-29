package com.company.scraper.worker.service;

import com.company.scraper.common.config.AppProperties;
import com.company.scraper.common.dto.ScrapeResultDto;
import com.company.scraper.common.dto.ScrapeTask;
import com.company.scraper.common.model.AttemptStatus;
import com.company.scraper.common.model.ChangeEvent;
import com.company.scraper.common.model.ScrapeAttempt;
import com.company.scraper.common.model.ScrapeResult;
import com.company.scraper.common.model.ScrapeTarget;
import com.company.scraper.common.repository.ChangeEventRepository;
import com.company.scraper.common.repository.ScrapeAttemptRepository;
import com.company.scraper.common.repository.ScrapeResultRepository;
import com.company.scraper.common.repository.ScrapeTargetRepository;
import com.company.scraper.worker.pipeline.ScrapePipeline;
import com.company.scraper.common.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScrapeProcessorService {

    private final ScrapePipeline pipeline;
    private final ScrapeTargetRepository targetRepository;
    private final ScrapeResultRepository resultRepository;
    private final ChangeEventRepository changeRepository;
    private final ScrapeAttemptRepository attemptRepository;
    private final KafkaTemplate<String, ScrapeTask> kafkaTemplate;
    private final AppProperties properties;
    private final ObjectMapper mapper;

    public ScrapeProcessorService(ScrapePipeline pipeline,
                                  ScrapeTargetRepository targetRepository,
                                  ScrapeResultRepository resultRepository,
                                  ChangeEventRepository changeRepository,
                                  ScrapeAttemptRepository attemptRepository,
                                  KafkaTemplate<String, ScrapeTask> kafkaTemplate,
                                  AppProperties properties,
                                  ObjectMapper mapper) {
        this.pipeline = pipeline;
        this.targetRepository = targetRepository;
        this.resultRepository = resultRepository;
        this.changeRepository = changeRepository;
        this.attemptRepository = attemptRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Transactional
    public void process(ScrapeTask task) {
        ScrapeTarget target = targetRepository.findById(task.targetId())
            .orElseThrow(() -> new IllegalArgumentException("Target not found"));

        ScrapeAttempt attempt = ScrapeAttempt.builder()
            .targetId(task.targetId())
            .status(AttemptStatus.RETRYING)
            .retryCount(task.attempt())
            .startedAt(Instant.now())
            .build();
        attempt = attemptRepository.save(attempt);

        try {
            ScrapeResultDto resultDto = pipeline.process(task, target.getContentHash());
            ScrapeResult result = ScrapeResult.builder()
                .jobId(task.jobId())
                .targetId(task.targetId())
                .httpStatus(resultDto.httpStatus())
                .durationMs(resultDto.durationMs())
                .fetchedAt(resultDto.fetchedAt())
                .changeDetected(resultDto.changeDetected())
                .payload(JsonUtils.toJson(mapper, resultDto.payload()))
                .contentHash(resultDto.contentHash())
                .build();
            resultRepository.save(result);

            if (resultDto.changeDetected()) {
                changeRepository.save(ChangeEvent.builder()
                    .targetId(task.targetId())
                    .previousHash(target.getContentHash())
                    .newHash(resultDto.contentHash())
                    .detectedAt(Instant.now())
                    .diff(null)
                    .build());
            }

            target.setLastScrapedAt(Instant.now());
            target.setContentHash(resultDto.contentHash());
            targetRepository.save(target);

            attempt.setStatus(AttemptStatus.SUCCESS);
            attempt.setFinishedAt(Instant.now());
            attemptRepository.save(attempt);
        } catch (Exception ex) {
            attempt.setStatus(AttemptStatus.FAILED);
            attempt.setErrorMessage(ex.getMessage());
            attempt.setFinishedAt(Instant.now());
            attemptRepository.save(attempt);
            handleRetry(task);
        }
    }

    private void handleRetry(ScrapeTask task) {
        int nextAttempt = task.attempt() + 1;
        if (nextAttempt <= 3) {
            ScrapeTask retry = new ScrapeTask(
                UUID.randomUUID().toString(),
                task.jobId(),
                task.targetId(),
                task.url(),
                task.method(),
                task.targetType(),
                task.headers(),
                task.body(),
                task.selectors(),
                task.pagination(),
                nextAttempt,
                Instant.now()
            );
            kafkaTemplate.send(properties.getKafka().getRetryTopic(), retry.correlationId(), retry);
        } else {
            kafkaTemplate.send(properties.getKafka().getDlqTopic(), task.correlationId(), task);
        }
    }
}
