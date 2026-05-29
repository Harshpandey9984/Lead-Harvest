package com.company.scraper.common.repository;

import com.company.scraper.common.model.JobStatus;
import com.company.scraper.common.model.ScrapeJob;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, Long> {
    List<ScrapeJob> findByStatus(JobStatus status);
    List<ScrapeJob> findByStatusAndNextRunAtBefore(JobStatus status, Instant time);
}
