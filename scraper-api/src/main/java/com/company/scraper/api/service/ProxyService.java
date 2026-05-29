package com.company.scraper.api.service;

import com.company.scraper.common.dto.ProxyRequest;
import com.company.scraper.common.model.ProxyEndpoint;
import com.company.scraper.common.model.ProxyStatus;
import com.company.scraper.common.repository.ProxyEndpointRepository;
import org.springframework.stereotype.Service;

@Service
public class ProxyService {

    private final ProxyEndpointRepository repository;

    public ProxyService(ProxyEndpointRepository repository) {
        this.repository = repository;
    }

    public ProxyEndpoint create(ProxyRequest request) {
        ProxyEndpoint proxy = ProxyEndpoint.builder()
            .host(request.host())
            .port(request.port())
            .username(request.username())
            .password(request.password())
            .protocol(request.protocol() == null ? "http" : request.protocol())
            .geo(request.geo())
            .status(ProxyStatus.HEALTHY)
            .successRate(1.0)
            .build();
        return repository.save(proxy);
    }
}
