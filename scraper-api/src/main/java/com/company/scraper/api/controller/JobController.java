package com.company.scraper.api.controller;

import com.company.scraper.api.service.JobService;
import com.company.scraper.api.service.ScrapeTaskPublisher;
import com.company.scraper.common.dto.CreateJobRequest;
import com.company.scraper.common.dto.CreateTargetRequest;
import com.company.scraper.common.dto.TriggerJobRequest;
import com.company.scraper.common.model.ScrapeJob;
import com.company.scraper.common.model.ScrapeTarget;
import com.company.scraper.common.repository.ScrapeTargetRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final ScrapeTargetRepository targetRepository;
    private final ScrapeTaskPublisher publisher;

    public JobController(JobService jobService,
                         ScrapeTargetRepository targetRepository,
                         ScrapeTaskPublisher publisher) {
        this.jobService = jobService;
        this.targetRepository = targetRepository;
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<ScrapeJob> createJob(@Valid @RequestBody CreateJobRequest request) {
        return ResponseEntity.ok(jobService.createJob(request));
    }

    @PostMapping("/{jobId}/targets")
    public ResponseEntity<ScrapeTarget> addTarget(@PathVariable Long jobId,
                                                  @Valid @RequestBody CreateTargetRequest request) {
        return ResponseEntity.ok(jobService.addTarget(jobId, request));
    }

    @GetMapping("/{jobId}/targets")
    public ResponseEntity<List<ScrapeTarget>> listTargets(@PathVariable Long jobId) {
        return ResponseEntity.ok(targetRepository.findByJobId(jobId));
    }

    @PostMapping("/{jobId}/trigger")
    public ResponseEntity<Void> trigger(@PathVariable Long jobId,
                                        @Valid @RequestBody TriggerJobRequest request) {
        ScrapeTarget target = targetRepository.findById(request.targetId())
            .orElseThrow(() -> new IllegalArgumentException("Target not found"));
        if (!target.getJob().getId().equals(jobId)) {
            throw new IllegalArgumentException("Target not part of job");
        }
        publisher.publish(target);
        return ResponseEntity.accepted().build();
    }
}
