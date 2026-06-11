package com.company.scraper.api.service;

import com.company.scraper.common.dto.MassExtractJobProgressResponse;
import com.company.scraper.common.model.MassExtractJob;
import com.company.scraper.common.model.MassExtractResult;
import com.company.scraper.common.repository.MassExtractJobRepository;
import com.company.scraper.common.repository.MassExtractResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MassExtractEngine {

    private final MassExtractJobRepository jobRepository;
    private final MassExtractResultRepository resultRepository;
    private final GooglePlacesService googlePlacesService;
    private final WebEnrichmentService webEnrichmentService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_PROGRESS_KEY_PREFIX = "mass-extract:job:";

    @Async("scrapeExecutor")
    public void executeJob(Long jobId) {
        log.info("Starting execution of Mass Extract Job ID: {}", jobId);
        
        Optional<MassExtractJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            log.error("Job ID {} not found, aborting execution", jobId);
            return;
        }

        MassExtractJob job = jobOpt.get();
        if ("PAUSED".equals(job.getStatus()) || "STOPPED".equals(job.getStatus())) {
            log.info("Job {} is already in status: {}, exiting", jobId, job.getStatus());
            return;
        }

        job.setStatus("RUNNING");
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Geocode location to lat/lng
            GooglePlacesService.LatLng latLng = googlePlacesService.geocode(job.getLocation());
            log.info("Geocoded location '{}' to lat: {}, lng: {}", job.getLocation(), latLng.lat, latLng.lng);

            // Step 2: Search for places
            List<MassExtractResult> searchResults = googlePlacesService.searchPlaces(
                    jobId, 
                    job.getQuery(), 
                    job.getLocation(), 
                    latLng.lat, 
                    latLng.lng, 
                    job.getRadiusKm(), 
                    job.getMaxResults()
            );

            log.info("Found {} total places for Job ID: {}", searchResults.size(), jobId);
            job.setTotalFound(searchResults.size());
            jobRepository.save(job);

            updateRedisProgress(job, startTime);

            // Step 3: Process and enrich each business website
            for (int i = 0; i < searchResults.size(); i++) {
                // Check if job status has changed dynamically (Pause/Stop/Cancel)
                Optional<MassExtractJob> currentJobState = jobRepository.findById(jobId);
                if (currentJobState.isPresent()) {
                    String currentStatus = currentJobState.get().getStatus();
                    if ("PAUSED".equals(currentStatus) || "STOPPED".equals(currentStatus) || "FAILED".equals(currentStatus)) {
                        log.info("Job ID {} status changed to {}, pausing/stopping extraction", jobId, currentStatus);
                        return;
                    }
                }

                MassExtractResult res = searchResults.get(i);
                
                // Check if result already exists and was enriched (resilience for resumes)
                Optional<MassExtractResult> existingResultOpt = resultRepository.findByJobIdAndPlaceId(jobId, res.getPlaceId());
                if (existingResultOpt.isPresent()) {
                    MassExtractResult existing = existingResultOpt.get();
                    if (existing.getEmail() != null || existing.getWebsiteUrl() == null || existing.getWebsiteUrl().isBlank()) {
                        log.info("Place {} already processed, skipping", res.getName());
                        
                        // Update progress counter anyway for the skip
                        job.setProcessedCount(job.getProcessedCount() + 1);
                        job.setSuccessCount(job.getSuccessCount() + 1);
                        job.setUpdatedAt(Instant.now());
                        jobRepository.save(job);
                        updateRedisProgress(job, startTime);
                        continue;
                    }
                    // If website url is present but email is null, re-attempt enrichment
                    res = existing;
                } else {
                    res = resultRepository.save(res);
                }

                boolean enriched = false;
                if (res.getWebsiteUrl() != null && !res.getWebsiteUrl().isBlank()) {
                    try {
                        // Enrich asynchronously but block this loop thread with .join() or .get() with a timeout
                        // This processes businesses sequentially but non-blockingly, keeping CPU usage reasonable.
                        CompletableFuture<Void> enrichmentFuture = webEnrichmentService.enrichResult(res);
                        // Wait up to 10 seconds for crawl to complete
                        enrichmentFuture.get(10, TimeUnit.SECONDS);
                        enriched = true;
                    } catch (Exception e) {
                        log.warn("Enrichment timed out or failed for place {}: {}", res.getName(), e.getMessage());
                    }
                }

                // Update job counters
                job.setProcessedCount(job.getProcessedCount() + 1);
                if (enriched || res.getWebsiteUrl() == null || res.getWebsiteUrl().isBlank()) {
                    job.setSuccessCount(job.getSuccessCount() + 1);
                } else {
                    job.setFailedCount(job.getFailedCount() + 1);
                }

                job.setUpdatedAt(Instant.now());
                job = jobRepository.save(job);

                // Publish progress metrics to Redis
                updateRedisProgress(job, startTime);
            }

            // Mark job as COMPLETED
            job.setStatus("COMPLETED");
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            updateRedisProgress(job, startTime);
            log.info("Mass Extract Job ID {} completed successfully!", jobId);

        } catch (Exception e) {
            log.error("Error executing Mass Extract Job ID {}: {}", jobId, e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            updateRedisProgress(job, startTime);
        }
    }

    private void updateRedisProgress(MassExtractJob job, long startTimeMs) {
        try {
            long durationMs = System.currentTimeMillis() - startTimeMs;
            double speed = 0.0;
            long etaSeconds = 0;

            int processed = job.getProcessedCount() != null ? job.getProcessedCount() : 0;
            int total = job.getTotalFound() != null ? job.getTotalFound() : 0;

            if (durationMs > 0 && processed > 0) {
                speed = (double) processed / (durationMs / 1000.0);
                if (speed > 0 && total > processed) {
                    etaSeconds = Math.round((total - processed) / speed);
                }
            }

            MassExtractJobProgressResponse progress = MassExtractJobProgressResponse.builder()
                    .id(job.getId())
                    .query(job.getQuery())
                    .location(job.getLocation())
                    .radiusKm(job.getRadiusKm())
                    .maxResults(job.getMaxResults())
                    .status(job.getStatus())
                    .totalFound(total)
                    .processedCount(processed)
                    .successCount(job.getSuccessCount() != null ? job.getSuccessCount() : 0)
                    .failedCount(job.getFailedCount() != null ? job.getFailedCount() : 0)
                    .errorMessage(job.getErrorMessage())
                    .createdAt(job.getCreatedAt())
                    .updatedAt(job.getUpdatedAt())
                    .speed(Math.round(speed * 100.0) / 100.0)
                    .etaSeconds(etaSeconds)
                    .build();

            String json = objectMapper.writeValueAsString(progress);
            String key = REDIS_PROGRESS_KEY_PREFIX + job.getId();
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(24));
        } catch (Exception e) {
            log.error("Failed to update Redis progress for job ID: {}", job.getId(), e);
        }
    }
}
