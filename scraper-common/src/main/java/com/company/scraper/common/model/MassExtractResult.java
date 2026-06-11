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
@Table(name = "mass_extract_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MassExtractResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "place_id", nullable = false)
    private String placeId;

    @Column(nullable = false)
    private String name;

    private String category;

    private String subcategory;

    @Column(length = 2000)
    private String description;

    private Double rating;

    @Column(name = "reviews_count")
    private Integer reviewsCount;

    private String phone;

    @Column(name = "secondary_phone")
    private String secondaryPhone;

    private String email;

    @Column(name = "website_url")
    private String websiteUrl;

    private String address;

    private String city;

    private String state;

    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    private Double latitude;

    private Double longitude;

    @Column(name = "maps_url")
    private String mapsUrl;

    @Column(name = "price_level")
    private Integer priceLevel;

    @Column(name = "business_status")
    private String businessStatus;

    @Column(name = "opening_hours", columnDefinition = "jsonb")
    private String openingHours;

    @Column(name = "open_now")
    private Boolean openNow;

    @Column(name = "permanently_closed")
    private Boolean permanentlyClosed;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(columnDefinition = "jsonb")
    private String photos;

    @Column(name = "reviews_summary", length = 4000)
    private String reviewsSummary;

    @Column(name = "reviews_keywords", columnDefinition = "jsonb")
    private String reviewsKeywords;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
