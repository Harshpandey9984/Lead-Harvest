package com.company.scraper.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proxy_endpoint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    private String username;

    private String password;

    private String protocol;

    private String geo;

    @Enumerated(EnumType.STRING)
    private ProxyStatus status;

    private Double successRate;

    private Instant lastCheckedAt;
}
