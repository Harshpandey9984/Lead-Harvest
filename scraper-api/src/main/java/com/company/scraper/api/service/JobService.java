package com.company.scraper.api.service;

import com.company.scraper.common.dto.CreateJobRequest;
import com.company.scraper.common.dto.CreateTargetRequest;
import com.company.scraper.common.model.JobStatus;
import com.company.scraper.common.model.ScrapeJob;
import com.company.scraper.common.model.ScrapeTarget;
import com.company.scraper.common.repository.ScrapeJobRepository;
import com.company.scraper.common.repository.ScrapeTargetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final ScrapeJobRepository jobRepository;
    private final ScrapeTargetRepository targetRepository;
    private final ObjectMapper mapper;

    public JobService(ScrapeJobRepository jobRepository,
                      ScrapeTargetRepository targetRepository,
                      ObjectMapper mapper) {
        this.jobRepository = jobRepository;
        this.targetRepository = targetRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ScrapeJob createJob(CreateJobRequest request) {
        ScrapeJob job = ScrapeJob.builder()
            .name(request.name())
            .status(JobStatus.ACTIVE)
            .schedule(request.schedule())
            .priority(request.priority())
            .maxConcurrency(request.maxConcurrency())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .nextRunAt(Instant.now())
            .build();
        return jobRepository.save(job);
    }

    @Transactional
    public ScrapeTarget addTarget(Long jobId, CreateTargetRequest request) {
        ScrapeJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        ScrapeTarget target = ScrapeTarget.builder()
            .job(job)
            .url(request.url())
            .method(request.method())
            .targetType(request.targetType())
            .headers(toJson(request.headers()))
            .body(request.body())
            .selectors(toJson(request.selectors()))
            .pagination(toJson(request.pagination()))
            .build();
        return targetRepository.save(target);
    }

    @Transactional(readOnly = true)
    public ScrapeJob getJob(Long jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }

    @Transactional(readOnly = true)
    public List<ScrapeJob> getAllJobs() {
        return jobRepository.findAll();
    }

    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid payload", ex);
        }
    }
}
