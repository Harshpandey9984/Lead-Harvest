package com.company.scraper.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProxyRequest(
    @NotBlank String host,
    @NotNull Integer port,
    String username,
    String password,
    String protocol,
    String geo
) { }
