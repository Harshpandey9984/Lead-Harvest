package com.company.scraper.common.repository;

import com.company.scraper.common.model.MassExtractResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MassExtractResultRepository extends JpaRepository<MassExtractResult, Long>, JpaSpecificationExecutor<MassExtractResult> {
    
    List<MassExtractResult> findByJobId(Long jobId);
    
    Page<MassExtractResult> findByJobId(Long jobId, Pageable pageable);
    
    long countByJobId(Long jobId);

    java.util.Optional<MassExtractResult> findByJobIdAndPlaceId(Long jobId, String placeId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByJobId(Long jobId);
}
