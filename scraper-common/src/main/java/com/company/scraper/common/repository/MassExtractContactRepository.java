package com.company.scraper.common.repository;

import com.company.scraper.common.model.MassExtractContact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MassExtractContactRepository extends JpaRepository<MassExtractContact, Long> {
    
    List<MassExtractContact> findByResultId(Long resultId);
    
    List<MassExtractContact> findByResultIdIn(List<Long> resultIds);
}
