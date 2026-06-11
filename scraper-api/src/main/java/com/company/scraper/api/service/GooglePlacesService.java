package com.company.scraper.api.service;

import com.company.scraper.common.model.MassExtractResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesService {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${google.maps.api-key:}")
    private String apiKey;

    public static class LatLng {
        public double lat;
        public double lng;
        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }

    public LatLng geocode(String location) {
        if (apiKey == null || apiKey.isBlank() || "mock".equalsIgnoreCase(apiKey)) {
            log.info("Using mock geocoding for location: {}", location);
            // Default center coordinates (e.g., Bhopal)
            return new LatLng(23.259933, 77.412615);
        }

        try {
            HttpUrl url = HttpUrl.parse("https://maps.googleapis.com/maps/api/geocode/json")
                    .newBuilder()
                    .addQueryParameter("address", location)
                    .addQueryParameter("key", apiKey)
                    .build();

            Request request = new Request.Builder().url(url).build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Geocoding API failed with status code: {}", response.code());
                    return new LatLng(23.259933, 77.412615);
                }
                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                String status = root.path("status").asText();
                if ("OK".equals(status) && root.path("results").size() > 0) {
                    JsonNode loc = root.path("results").get(0).path("geometry").path("location");
                    return new LatLng(loc.path("lat").asDouble(), loc.path("lng").asDouble());
                } else {
                    log.warn("Geocoding failed with status: {}, returning default coords", status);
                }
            }
        } catch (Exception e) {
            log.error("Error calling Geocoding API: {}", e.getMessage(), e);
        }
        return new LatLng(23.259933, 77.412615);
    }

    public List<MassExtractResult> searchPlaces(Long jobId, String query, String location, double lat, double lng, int radiusKm, int maxResults) {
        if (apiKey == null || apiKey.isBlank() || "mock".equalsIgnoreCase(apiKey)) {
            log.info("Using mock place search for query: {}, location: {}", query, location);
            return generateMockPlaces(jobId, query, location, maxResults);
        }

        List<MassExtractResult> results = new ArrayList<>();
        String pageToken = null;
        int radiusMeters = radiusKm * 1000;

        try {
            do {
                HttpUrl.Builder urlBuilder = HttpUrl.parse("https://maps.googleapis.com/maps/api/place/textsearch/json")
                        .newBuilder()
                        .addQueryParameter("query", query + " in " + location)
                        .addQueryParameter("location", lat + "," + lng)
                        .addQueryParameter("radius", String.valueOf(radiusMeters))
                        .addQueryParameter("key", apiKey);

                if (pageToken != null) {
                    urlBuilder.addQueryParameter("pagetoken", pageToken);
                    // Google requires a short delay before page token becomes active
                    Thread.sleep(2000);
                }

                Request request = new Request.Builder().url(urlBuilder.build()).build();
                try (Response response = okHttpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        log.error("Place Search API failed with code: {}", response.code());
                        break;
                    }

                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);
                    String status = root.path("status").asText();
                    if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                        log.error("Place Search API status: {}", status);
                        break;
                    }

                    JsonNode resultsNode = root.path("results");
                    for (JsonNode placeNode : resultsNode) {
                        if (results.size() >= maxResults) {
                            break;
                        }
                        String placeId = placeNode.path("place_id").asText();
                        MassExtractResult details = getPlaceDetails(jobId, placeId);
                        if (details != null) {
                            results.add(details);
                        } else {
                            // Fallback to what we can get from the search result if details API fails or quota hit
                            results.add(mapSearchResultToEntity(jobId, placeNode));
                        }
                    }

                    pageToken = root.path("next_page_token").asText(null);
                    if (pageToken == null || pageToken.isBlank()) {
                        break;
                    }
                }
            } while (results.size() < maxResults);

        } catch (Exception e) {
            log.error("Error calling Places Search API: {}", e.getMessage(), e);
        }

        return results;
    }

    public MassExtractResult getPlaceDetails(Long jobId, String placeId) {
        if (apiKey == null || apiKey.isBlank() || "mock".equalsIgnoreCase(apiKey)) {
            return null;
        }

        try {
            HttpUrl url = HttpUrl.parse("https://maps.googleapis.com/maps/api/place/details/json")
                    .newBuilder()
                    .addQueryParameter("place_id", placeId)
                    .addQueryParameter("fields", "name,formatted_phone_number,website,rating,reviews,geometry,formatted_address,price_level,business_status,opening_hours,photos,editorial_summary")
                    .addQueryParameter("key", apiKey)
                    .build();

            Request request = new Request.Builder().url(url).build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Place Details API failed for placeId: {}, status: {}", placeId, response.code());
                    return null;
                }
                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                String status = root.path("status").asText();
                if ("OK".equals(status)) {
                    JsonNode resultNode = root.path("result");
                    return mapPlaceDetailsToEntity(jobId, placeId, resultNode);
                } else {
                    log.warn("Place Details status not OK: {} for placeId: {}", status, placeId);
                }
            }
        } catch (Exception e) {
            log.error("Error getting Place Details for placeId: {}", placeId, e);
        }
        return null;
    }

    private MassExtractResult mapSearchResultToEntity(Long jobId, JsonNode node) {
        JsonNode loc = node.path("geometry").path("location");
        return MassExtractResult.builder()
                .jobId(jobId)
                .placeId(node.path("place_id").asText())
                .name(node.path("name").asText())
                .rating(node.has("rating") ? node.path("rating").asDouble() : null)
                .reviewsCount(node.has("user_ratings_total") ? node.path("user_ratings_total").asInt() : 0)
                .address(node.path("formatted_address").asText())
                .latitude(loc.path("lat").asDouble())
                .longitude(loc.path("lng").asDouble())
                .businessStatus(node.path("business_status").asText("OPERATIONAL"))
                .mapsUrl("https://www.google.com/maps/place/?q=place_id:" + node.path("place_id").asText())
                .createdAt(Instant.now())
                .build();
    }

    private MassExtractResult mapPlaceDetailsToEntity(Long jobId, String placeId, JsonNode node) {
        JsonNode loc = node.path("geometry").path("location");
        MassExtractResult result = MassExtractResult.builder()
                .jobId(jobId)
                .placeId(placeId)
                .name(node.path("name").asText())
                .rating(node.has("rating") ? node.path("rating").asDouble() : null)
                .reviewsCount(node.has("reviews") ? node.path("reviews").size() : 0)
                .phone(node.path("formatted_phone_number").asText(null))
                .websiteUrl(node.path("website").asText(null))
                .address(node.path("formatted_address").asText(null))
                .latitude(loc.path("lat").asDouble())
                .longitude(loc.path("lng").asDouble())
                .mapsUrl("https://www.google.com/maps/place/?q=place_id:" + placeId)
                .priceLevel(node.has("price_level") ? node.path("price_level").asInt() : null)
                .businessStatus(node.path("business_status").asText("OPERATIONAL"))
                .createdAt(Instant.now())
                .build();

        // Extract address components
        JsonNode addrComponents = node.path("address_components");
        if (addrComponents.isArray()) {
            for (JsonNode comp : addrComponents) {
                JsonNode types = comp.path("types");
                boolean isLocality = false;
                boolean isState = false;
                boolean isCountry = false;
                boolean isPostal = false;
                for (JsonNode t : types) {
                    String ts = t.asText();
                    if ("locality".equals(ts)) isLocality = true;
                    if ("administrative_area_level_1".equals(ts)) isState = true;
                    if ("country".equals(ts)) isCountry = true;
                    if ("postal_code".equals(ts)) isPostal = true;
                }
                if (isLocality) result.setCity(comp.path("long_name").asText());
                if (isState) result.setState(comp.path("long_name").asText());
                if (isCountry) result.setCountry(comp.path("long_name").asText());
                if (isPostal) result.setPostalCode(comp.path("long_name").asText());
            }
        }

        // Handle opening hours JSON serialization
        if (node.has("opening_hours")) {
            result.setOpeningHours(node.path("opening_hours").toString());
            result.setOpenNow(node.path("opening_hours").path("open_now").asBoolean(false));
        }

        // Handle reviews summary/keywords if editorial summary exists
        if (node.has("editorial_summary")) {
            result.setDescription(node.path("editorial_summary").path("overview").asText());
        }

        // Store photos references as JSON
        if (node.has("photos")) {
            result.setPhotos(node.path("photos").toString());
            if (node.path("photos").size() > 0) {
                result.setLogoUrl(node.path("photos").get(0).path("photo_reference").asText());
            }
        }

        return result;
    }

    private List<MassExtractResult> generateMockPlaces(Long jobId, String query, String location, int maxResults) {
        List<MassExtractResult> list = new ArrayList<>();
        Random rand = new Random();
        String[] subcats = {"Premium", "Standard", "Budget", "Boutique", "Local Favorite"};
        String[] keywords = {"excellent service", "highly recommended", "great value", "friendly staff", "clean environment"};

        for (int i = 1; i <= maxResults; i++) {
            double rating = 3.5 + (1.5 * rand.nextDouble());
            int reviews = 10 + rand.nextInt(480);
            String placeId = "ChIJ_mock_" + query.toLowerCase().replaceAll("\\s+", "") + "_" + i + "_" + System.currentTimeMillis();
            String name = query + " " + getMockNameSuffix(query) + " " + i;

            // Generate coordinates centered near the geocoded location
            double latOffset = (rand.nextDouble() - 0.5) * 0.05;
            double lngOffset = (rand.nextDouble() - 0.5) * 0.05;
            double placeLat = 23.259933 + latOffset;
            double placeLng = 77.412615 + lngOffset;

            String cleanName = name.toLowerCase().replaceAll("[^a-z0-9]", "-");
            String website = "http://www." + cleanName + ".com";

            MassExtractResult res = MassExtractResult.builder()
                    .jobId(jobId)
                    .placeId(placeId)
                    .name(name)
                    .category(query)
                    .subcategory(subcats[rand.nextInt(subcats.length)])
                    .description("A wonderful " + query.toLowerCase() + " located in the heart of " + location + ". Known for great atmosphere.")
                    .rating(Math.round(rating * 10.0) / 10.0)
                    .reviewsCount(reviews)
                    .phone("+91 755 " + (5000000 + rand.nextInt(999999)))
                    .websiteUrl(website)
                    .address(i * 12 + ", Main St, Arera Colony, " + location + ", Madhya Pradesh, India")
                    .city(location)
                    .state("Madhya Pradesh")
                    .country("India")
                    .postalCode(String.valueOf(462000 + rand.nextInt(50)))
                    .latitude(placeLat)
                    .longitude(placeLng)
                    .mapsUrl("https://maps.google.com/?q=" + placeLat + "," + placeLng)
                    .priceLevel(1 + rand.nextInt(4))
                    .businessStatus("OPERATIONAL")
                    .createdAt(Instant.now())
                    .openNow(rand.nextBoolean())
                    .reviewsSummary("Many customers mention: " + keywords[rand.nextInt(keywords.length)])
                    .build();

            list.add(res);
        }
        return list;
    }

    private String getMockNameSuffix(String query) {
        String lower = query.toLowerCase();
        if (lower.contains("cafe") || lower.contains("coffee")) return "Hub";
        if (lower.contains("restaurant") || lower.contains("food")) return "Kitchen";
        if (lower.contains("hotel")) return "Residency";
        if (lower.contains("gym") || lower.contains("fitness")) return "Arena";
        if (lower.contains("school") || lower.contains("academy")) return "International";
        if (lower.contains("hospital") || lower.contains("clinic")) return "Care";
        if (lower.contains("salon") || lower.contains("spa")) return "Studio";
        if (lower.contains("dentist")) return "Dental Care";
        if (lower.contains("real estate")) return "Homes";
        if (lower.contains("software") || lower.contains("it")) return "Solutions";
        return "Group";
    }
}
