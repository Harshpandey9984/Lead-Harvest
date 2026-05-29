package com.company.scraper.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scrape_target")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ScrapeJob job;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(columnDefinition = "jsonb")
    private String headers;

    @Column(columnDefinition = "jsonb")
    private String body;

    @Column(columnDefinition = "jsonb")
    private String selectors;

    @Column(columnDefinition = "jsonb")
    private String pagination;

    private Instant lastScrapedAt;

    private String contentHash;
}
