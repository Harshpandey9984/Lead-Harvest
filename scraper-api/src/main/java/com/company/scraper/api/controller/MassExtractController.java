package com.company.scraper.api.controller;

import com.company.scraper.api.service.MassExportService;
import com.company.scraper.api.service.MassExtractEngine;
import com.company.scraper.common.dto.CreateMassExtractJobRequest;
import com.company.scraper.common.dto.MassExtractAnalyticsResponse;
import com.company.scraper.common.dto.MassExtractJobProgressResponse;
import com.company.scraper.common.dto.MassExtractResultResponse;
import com.company.scraper.common.model.MassExtractJob;
import com.company.scraper.common.model.MassExtractResult;
import com.company.scraper.common.repository.MassExtractJobRepository;
import com.company.scraper.common.repository.MassExtractResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/mass-extract")
@RequiredArgsConstructor
@Slf4j
public class MassExtractController {

    private final MassExtractJobRepository jobRepository;
    private final MassExtractResultRepository resultRepository;
    private final MassExtractEngine massExtractEngine;
    private final MassExportService massExportService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_PROGRESS_KEY_PREFIX = "mass-extract:job:";

    @PostMapping
    public ResponseEntity<MassExtractJob> startJob(@Valid @RequestBody CreateMassExtractJobRequest request) {
        log.info("Request received to start Mass Extract job: {}", request);
        
        MassExtractJob job = MassExtractJob.builder()
                .query(request.getQuery())
                .location(request.getLocation())
                .radiusKm(request.getRadiusKm() != null ? request.getRadiusKm() : 10)
                .maxResults(request.getMaxResults() != null ? request.getMaxResults() : 50)
                .status("PENDING")
                .totalFound(0)
                .processedCount(0)
                .successCount(0)
                .failedCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        job = jobRepository.save(job);
        
        // Trigger execution asynchronously
        massExtractEngine.executeJob(job.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @GetMapping("/history")
    public ResponseEntity<List<MassExtractJob>> getJobHistory() {
        return ResponseEntity.ok(jobRepository.findAllOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<MassExtractJobProgressResponse> getJobProgress(@PathVariable Long id) {
        String key = REDIS_PROGRESS_KEY_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            try {
                MassExtractJobProgressResponse progress = objectMapper.readValue(cached.toString(), MassExtractJobProgressResponse.class);
                return ResponseEntity.ok(progress);
            } catch (Exception e) {
                log.warn("Failed to parse cached progress for job ID: {}, loading from DB", id, e);
            }
        }

        // Fallback to database if Redis is empty
        MassExtractJob job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));

        MassExtractJobProgressResponse progress = MassExtractJobProgressResponse.builder()
                .id(job.getId())
                .query(job.getQuery())
                .location(job.getLocation())
                .radiusKm(job.getRadiusKm())
                .maxResults(job.getMaxResults())
                .status(job.getStatus())
                .totalFound(job.getTotalFound() != null ? job.getTotalFound() : 0)
                .processedCount(job.getProcessedCount() != null ? job.getProcessedCount() : 0)
                .successCount(job.getSuccessCount() != null ? job.getSuccessCount() : 0)
                .failedCount(job.getFailedCount() != null ? job.getFailedCount() : 0)
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .speed(0.0)
                .etaSeconds(0L)
                .build();

        return ResponseEntity.ok(progress);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pauseJob(@PathVariable Long id) {
        log.info("Pausing Mass Extract job: {}", id);
        MassExtractJob job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));

        if ("RUNNING".equals(job.getStatus()) || "PENDING".equals(job.getStatus())) {
            job.setStatus("PAUSED");
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            
            // Invalidate/update Redis key
            invalidateRedisProgress(job);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Void> resumeJob(@PathVariable Long id) {
        log.info("Resuming Mass Extract job: {}", id);
        MassExtractJob job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));

        if ("PAUSED".equals(job.getStatus()) || "FAILED".equals(job.getStatus())) {
            job.setStatus("RUNNING");
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            
            invalidateRedisProgress(job);
            
            // Re-trigger execution
            massExtractEngine.executeJob(job.getId());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stopJob(@PathVariable Long id) {
        log.info("Stopping Mass Extract job: {}", id);
        MassExtractJob job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));

        if (!"COMPLETED".equals(job.getStatus()) && !"FAILED".equals(job.getStatus())) {
            job.setStatus("STOPPED");
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            
            invalidateRedisProgress(job);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/rerun")
    public ResponseEntity<Void> rerunJob(@PathVariable Long id) {
        log.info("Rerunning Mass Extract job: {}", id);
        MassExtractJob job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));

        job.setStatus("PENDING");
        job.setProcessedCount(0);
        job.setSuccessCount(0);
        job.setFailedCount(0);
        job.setErrorMessage(null);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        // Delete old results for a clean rerun
        resultRepository.deleteByJobId(id);

        invalidateRedisProgress(job);

        // Re-trigger execution
        massExtractEngine.executeJob(job.getId());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<Page<MassExtractResultResponse>> getJobResults(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Boolean hasEmail,
            @RequestParam(required = false) Boolean hasWebsite,
            @RequestParam(required = false) Boolean hasPhone) {

        Pageable pageable = PageRequest.of(page, size);

        Specification<MassExtractResult> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("jobId"), id));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("category")), pattern),
                        cb.like(cb.lower(root.get("city")), pattern)
                ));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            if (Boolean.TRUE.equals(hasEmail)) {
                predicates.add(cb.isNotNull(root.get("email")));
                predicates.add(cb.notEqual(root.get("email"), ""));
            }

            if (Boolean.TRUE.equals(hasWebsite)) {
                predicates.add(cb.isNotNull(root.get("websiteUrl")));
                predicates.add(cb.notEqual(root.get("websiteUrl"), ""));
            }

            if (Boolean.TRUE.equals(hasPhone)) {
                predicates.add(cb.isNotNull(root.get("phone")));
                predicates.add(cb.notEqual(root.get("phone"), ""));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MassExtractResult> resultsPage = resultRepository.findAll(spec, pageable);
        
        // Map to response DTOs using our batch mapper to load socials/contacts
        List<MassExtractResultResponse> allJobResponses = massExportService.getResultResponses(id);
        
        // Filter the batch-loaded responses to match the paginated DB results
        Set<Long> resultIdsOnPage = new HashSet<>();
        for (MassExtractResult r : resultsPage.getContent()) {
            resultIdsOnPage.add(r.getId());
        }

        List<MassExtractResultResponse> paginatedResponses = allJobResponses.stream()
                .filter(res -> resultIdsOnPage.contains(res.getId()))
                .toList();

        Page<MassExtractResultResponse> responsePage = new PageImpl<>(
                paginatedResponses,
                pageable,
                resultsPage.getTotalElements()
        );

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<MassExtractAnalyticsResponse> getJobAnalytics(@PathVariable Long id) {
        List<MassExtractResult> results = resultRepository.findByJobId(id);
        
        long total = results.size();
        long withWebsite = 0;
        long withEmail = 0;
        double sumRating = 0;
        long ratingCount = 0;

        Map<String, Long> categoryDist = new HashMap<>();
        Map<String, Long> cityDist = new HashMap<>();
        
        // Ratings brackets: e.g. "4.5-5.0", "4.0-4.4", etc.
        Map<String, Long> ratingDist = new LinkedHashMap<>();
        ratingDist.put("4.5-5.0", 0L);
        ratingDist.put("4.0-4.4", 0L);
        ratingDist.put("3.5-3.9", 0L);
        ratingDist.put("3.0-3.4", 0L);
        ratingDist.put("< 3.0", 0L);

        for (MassExtractResult r : results) {
            if (r.getWebsiteUrl() != null && !r.getWebsiteUrl().isBlank()) {
                withWebsite++;
            }
            if (r.getEmail() != null && !r.getEmail().isBlank()) {
                withEmail++;
            }
            if (r.getRating() != null) {
                sumRating += r.getRating();
                ratingCount++;

                double rate = r.getRating();
                if (rate >= 4.5) ratingDist.put("4.5-5.0", ratingDist.get("4.5-5.0") + 1);
                else if (rate >= 4.0) ratingDist.put("4.0-4.4", ratingDist.get("4.0-4.4") + 1);
                else if (rate >= 3.5) ratingDist.put("3.5-3.9", ratingDist.get("3.5-3.9") + 1);
                else if (rate >= 3.0) ratingDist.put("3.0-3.4", ratingDist.get("3.0-3.4") + 1);
                else ratingDist.put("< 3.0", ratingDist.get("< 3.0") + 1);
            }

            if (r.getCategory() != null && !r.getCategory().isBlank()) {
                categoryDist.put(r.getCategory(), categoryDist.getOrDefault(r.getCategory(), 0L) + 1);
            }
            if (r.getCity() != null && !r.getCity().isBlank()) {
                cityDist.put(r.getCity(), cityDist.getOrDefault(r.getCity(), 0L) + 1);
            }
        }

        double avgRating = ratingCount > 0 ? (sumRating / ratingCount) : 0.0;
        avgRating = Math.round(avgRating * 100.0) / 100.0;

        // Populate mock monthly trends since it's a single snapshot
        Map<String, Long> monthlyTrends = new LinkedHashMap<>();
        monthlyTrends.put("June 2026", total);

        MassExtractAnalyticsResponse analytics = MassExtractAnalyticsResponse.builder()
                .totalBusinesses(total)
                .totalSearches(1)
                .averageRating(avgRating)
                .withWebsite(withWebsite)
                .withoutWebsite(total - withWebsite)
                .withEmail(withEmail)
                .withoutEmail(total - withEmail)
                .categoryDistribution(categoryDist)
                .ratingDistribution(ratingDist)
                .cityDistribution(cityDist)
                .monthlyTrends(monthlyTrends)
                .build();

        return ResponseEntity.ok(analytics);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        log.info("Deleting Mass Extract Job ID: {}", id);
        if (jobRepository.existsById(id)) {
            jobRepository.deleteById(id);
            String key = REDIS_PROGRESS_KEY_PREFIX + id;
            redisTemplate.delete(key);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportLeads(
            @PathVariable Long id,
            @RequestParam(defaultValue = "excel") String format) {

        log.info("Exporting leads for Job ID: {}, format: {}", id, format);

        try {
            byte[] fileBytes;
            String filename;
            MediaType mediaType;

            if ("csv".equalsIgnoreCase(format)) {
                fileBytes = massExportService.exportToCsv(id);
                filename = "leads_job_" + id + ".csv";
                mediaType = MediaType.parseMediaType("text/csv");
            } else if ("json".equalsIgnoreCase(format)) {
                fileBytes = massExportService.exportToJson(id);
                filename = "leads_job_" + id + ".json";
                mediaType = MediaType.APPLICATION_JSON;
            } else {
                fileBytes = massExportService.exportToExcel(id);
                filename = "leads_job_" + id + ".xlsx";
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("Failed to generate export file for job: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void invalidateRedisProgress(MassExtractJob job) {
        try {
            String key = REDIS_PROGRESS_KEY_PREFIX + job.getId();
            
            MassExtractJobProgressResponse progress = MassExtractJobProgressResponse.builder()
                    .id(job.getId())
                    .query(job.getQuery())
                    .location(job.getLocation())
                    .radiusKm(job.getRadiusKm())
                    .maxResults(job.getMaxResults())
                    .status(job.getStatus())
                    .totalFound(job.getTotalFound() != null ? job.getTotalFound() : 0)
                    .processedCount(job.getProcessedCount() != null ? job.getProcessedCount() : 0)
                    .successCount(job.getSuccessCount() != null ? job.getSuccessCount() : 0)
                    .failedCount(job.getFailedCount() != null ? job.getFailedCount() : 0)
                    .errorMessage(job.getErrorMessage())
                    .createdAt(job.getCreatedAt())
                    .updatedAt(job.getUpdatedAt())
                    .speed(0.0)
                    .etaSeconds(0L)
                    .build();

            String json = objectMapper.writeValueAsString(progress);
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(24));
        } catch (Exception e) {
            log.error("Failed to invalidate/update Redis progress status for job ID: {}", job.getId(), e);
        }
    }
}
