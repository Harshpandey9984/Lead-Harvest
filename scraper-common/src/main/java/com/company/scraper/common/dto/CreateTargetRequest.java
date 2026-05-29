package com.company.scraper.common.dto;

import com.company.scraper.common.model.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateTargetRequest(
    @NotBlank String url,
    @NotBlank String method,
    @NotNull TargetType targetType,
    Map<String, String> headers,
    String body,
    Map<String, String> selectors,
    Map<String, Object> pagination
) { }
