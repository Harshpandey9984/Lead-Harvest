package com.company.scraper.api.controller;

import com.company.scraper.api.service.ProxyService;
import com.company.scraper.common.dto.ProxyRequest;
import com.company.scraper.common.model.ProxyEndpoint;
import com.company.scraper.common.repository.ProxyEndpointRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxies")
public class ProxyController {

    private final ProxyService proxyService;
    private final ProxyEndpointRepository repository;

    public ProxyController(ProxyService proxyService, ProxyEndpointRepository repository) {
        this.proxyService = proxyService;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<ProxyEndpoint> create(@Valid @RequestBody ProxyRequest request) {
        return ResponseEntity.ok(proxyService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ProxyEndpoint>> list() {
        return ResponseEntity.ok(repository.findAll());
    }
}
