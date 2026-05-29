package com.company.scraper.common.dto;

import com.company.scraper.common.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record NotificationRequest(
    @NotNull NotificationChannel channel,
    @NotBlank String recipient,
    Map<String, Object> payload
) { }
