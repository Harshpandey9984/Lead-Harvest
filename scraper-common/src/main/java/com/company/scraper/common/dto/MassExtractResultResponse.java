package com.company.scraper.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MassExtractResultResponse {
    private Long id;
    private Long jobId;
    private String placeId;
    private String name;
    private String category;
    private String subcategory;
    private String description;
    private Double rating;
    private Integer reviewsCount;
    private String phone;
    private String secondaryPhone;
    private String email;
    private String websiteUrl;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String mapsUrl;
    private Integer priceLevel;
    private String businessStatus;
    private String openingHours;
    private Boolean openNow;
    private Boolean permanentlyClosed;
    private String logoUrl;
    private String photos;
    private String reviewsSummary;
    private String reviewsKeywords;
    private List<String> socialLinks;
    private List<String> scrapedEmails;
    private List<String> scrapedPhones;
}
