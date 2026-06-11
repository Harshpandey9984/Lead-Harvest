package com.company.scraper.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mass_extract_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MassExtractJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String query;

    @Column(nullable = false)
    private String location;

    @Column(name = "radius_km", nullable = false)
    private Integer radiusKm;

    @Column(name = "max_results", nullable = false)
    private Integer maxResults;

    @Column(nullable = false)
    private String status;

    @Column(name = "total_found")
    private Integer totalFound;

    @Column(name = "processed_count")
    private Integer processedCount;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
