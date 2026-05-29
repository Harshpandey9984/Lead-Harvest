package com.company.scraper.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateJobRequest(
    @NotBlank String name,
    @NotBlank String schedule,
    @NotNull Integer priority,
    @NotNull Integer maxConcurrency
) { }
