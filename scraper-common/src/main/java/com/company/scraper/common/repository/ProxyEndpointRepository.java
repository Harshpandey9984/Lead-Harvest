package com.company.scraper.common.repository;

import com.company.scraper.common.model.ProxyEndpoint;
import com.company.scraper.common.model.ProxyStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProxyEndpointRepository extends JpaRepository<ProxyEndpoint, Long> {
    List<ProxyEndpoint> findByStatus(ProxyStatus status);
}
