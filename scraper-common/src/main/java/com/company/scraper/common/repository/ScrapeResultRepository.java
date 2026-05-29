package com.company.scraper.common.repository;

import com.company.scraper.common.model.ScrapeResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapeResultRepository extends JpaRepository<ScrapeResult, Long> {
    List<ScrapeResult> findTop100ByTargetIdOrderByFetchedAtDesc(Long targetId);
}
