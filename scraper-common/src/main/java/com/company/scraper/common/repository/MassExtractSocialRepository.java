package com.company.scraper.common.repository;

import com.company.scraper.common.model.MassExtractSocial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MassExtractSocialRepository extends JpaRepository<MassExtractSocial, Long> {
    
    List<MassExtractSocial> findByResultId(Long resultId);
    
    List<MassExtractSocial> findByResultIdIn(List<Long> resultIds);
}
