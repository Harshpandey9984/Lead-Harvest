package com.company.scraper.common.repository;

import com.company.scraper.common.entity.ScrapingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrapingTaskRepository extends JpaRepository<ScrapingTask, Long> {
    List<ScrapingTask> findByStatus(String status);
    List<ScrapingTask> findByStatusAndRetryCountLessThan(String status, Integer retryCount);
}
