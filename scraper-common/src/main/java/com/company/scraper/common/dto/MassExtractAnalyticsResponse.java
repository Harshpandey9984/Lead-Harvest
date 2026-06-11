package com.company.scraper.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MassExtractAnalyticsResponse {
    private long totalBusinesses;
    private long totalSearches;
    private double averageRating;
    private long withWebsite;
    private long withoutWebsite;
    private long withEmail;
    private long withoutEmail;
    
    // Distribution charts maps
    private Map<String, Long> categoryDistribution;
    private Map<String, Long> ratingDistribution; // e.g. "4.0-5.0" -> 140, "3.0-4.0" -> 50
    private Map<String, Long> cityDistribution;
    private Map<String, Long> monthlyTrends; // e.g. "2026-06" -> 850
}
