package com.company.scraper.common.repository;

import com.company.scraper.common.model.ScrapeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapeAttemptRepository extends JpaRepository<ScrapeAttempt, Long> {
}
