package com.company.scraper.common.dto;

import jakarta.validation.constraints.NotNull;

public record TriggerJobRequest(@NotNull Long targetId) { }
