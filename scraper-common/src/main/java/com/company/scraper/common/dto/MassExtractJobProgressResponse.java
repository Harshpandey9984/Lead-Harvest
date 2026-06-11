package com.company.scraper.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MassExtractJobProgressResponse {
    private Long id;
    private String query;
    private String location;
    private Integer radiusKm;
    private Integer maxResults;
    private String status;
    private Integer totalFound;
    private Integer processedCount;
    private Integer successCount;
    private Integer failedCount;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private Double speed; // results per second
    private Long etaSeconds;
}
