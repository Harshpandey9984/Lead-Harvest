package com.company.scraper.common.repository;

import com.company.scraper.common.model.ScrapeTarget;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapeTargetRepository extends JpaRepository<ScrapeTarget, Long> {
    List<ScrapeTarget> findByJobId(Long jobId);
}
