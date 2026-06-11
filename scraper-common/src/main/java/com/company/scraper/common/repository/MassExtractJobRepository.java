package com.company.scraper.common.repository;

import com.company.scraper.common.model.MassExtractJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MassExtractJobRepository extends JpaRepository<MassExtractJob, Long> {
    
    List<MassExtractJob> findByStatus(String status);
    
    @Query("SELECT j FROM MassExtractJob j ORDER BY j.createdAt DESC")
    List<MassExtractJob> findAllOrderByCreatedAtDesc();
}
