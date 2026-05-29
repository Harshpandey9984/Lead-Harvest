package com.company.scraper.api.controller;

import com.company.scraper.common.model.ScrapeResult;
import com.company.scraper.common.repository.ScrapeResultRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ScrapeResultRepository repository;

    public ResultController(ScrapeResultRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/targets/{targetId}")
    public ResponseEntity<List<ScrapeResult>> list(@PathVariable Long targetId) {
        return ResponseEntity.ok(repository.findTop100ByTargetIdOrderByFetchedAtDesc(targetId));
    }
}
