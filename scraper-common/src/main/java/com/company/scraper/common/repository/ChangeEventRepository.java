package com.company.scraper.common.repository;

import com.company.scraper.common.model.ChangeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeEventRepository extends JpaRepository<ChangeEvent, Long> {
}
