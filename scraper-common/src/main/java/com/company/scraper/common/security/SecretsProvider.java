package com.company.scraper.common.security;

import org.springframework.stereotype.Component;

@Component
public class SecretsProvider {

    public String getSecret(String key) {
        String envValue = System.getenv(key);
        if (envValue == null || envValue.isBlank()) {
            throw new IllegalStateException("Missing secret: " + key);
        }
        return envValue;
    }
}
