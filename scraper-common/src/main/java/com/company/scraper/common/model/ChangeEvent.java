package com.company.scraper.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "change_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long targetId;

    @Column(nullable = false)
    private String previousHash;

    @Column(nullable = false)
    private String newHash;

    @Column(nullable = false)
    private Instant detectedAt;

    @Column(columnDefinition = "jsonb")
    private String diff;
}
