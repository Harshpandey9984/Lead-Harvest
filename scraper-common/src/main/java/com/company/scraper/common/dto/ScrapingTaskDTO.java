package com.company.scraper.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapingTaskDTO {
    private Long id;
    private String url;
    private String status;
    private String result;
    private String errorMessage;
    private Integer retryCount;
    private String schedulerExpression;
}
