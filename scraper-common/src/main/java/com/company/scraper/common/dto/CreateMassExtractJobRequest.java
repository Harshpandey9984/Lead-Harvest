package com.company.scraper.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMassExtractJobRequest {
    private String query;
    private String location;
    private Integer radiusKm;
    private Integer maxResults;
}
