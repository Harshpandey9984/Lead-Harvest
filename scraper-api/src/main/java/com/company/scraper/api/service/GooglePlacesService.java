package com.company.scraper.api.service;

import com.company.scraper.common.model.MassExtractResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

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

    // =========================================================
    // GEOCODING
    // =========================================================

    public LatLng geocode(String location) {
        // Always try Nominatim first (free, no API key needed)
        LatLng nominatimResult = geocodeViaNominatim(location);
        if (nominatimResult != null) {
            return nominatimResult;
        }

        // Fallback to Google Geocoding if API key is available
        if (apiKey != null && !apiKey.isBlank() && !"mock".equalsIgnoreCase(apiKey)) {
            return geocodeViaGoogle(location);
        }

        log.warn("Geocoding failed for location: {}, using fallback coordinates (New Delhi)", location);
        return new LatLng(28.6139, 77.2090);
    }

    private LatLng geocodeViaNominatim(String location) {
        try {
            HttpUrl url = HttpUrl.parse("https://nominatim.openstreetmap.org/search")
                    .newBuilder()
                    .addQueryParameter("q", location)
                    .addQueryParameter("format", "json")
                    .addQueryParameter("limit", "1")
                    .addQueryParameter("addressdetails", "1")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "LeadHarvest/1.0 (business-scraper)")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Nominatim geocoding failed with HTTP {}", response.code());
                    return null;
                }
                String body = response.body().string();
                JsonNode results = objectMapper.readTree(body);
                if (results.isArray() && results.size() > 0) {
                    JsonNode first = results.get(0);
                    double lat = first.path("lat").asDouble();
                    double lng = first.path("lon").asDouble();
                    log.info("Nominatim geocoded '{}' => lat={}, lng={}", location, lat, lng);
                    return new LatLng(lat, lng);
                }
            }
        } catch (Exception e) {
            log.error("Error calling Nominatim: {}", e.getMessage());
        }
        return null;
    }

    private LatLng geocodeViaGoogle(String location) {
        try {
            HttpUrl url = HttpUrl.parse("https://maps.googleapis.com/maps/api/geocode/json")
                    .newBuilder()
                    .addQueryParameter("address", location)
                    .addQueryParameter("key", apiKey)
                    .build();

            Request request = new Request.Builder().url(url).build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Google Geocoding API failed with status code: {}", response.code());
                    return null;
                }
                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                String status = root.path("status").asText();
                if ("OK".equals(status) && root.path("results").size() > 0) {
                    JsonNode loc = root.path("results").get(0).path("geometry").path("location");
                    return new LatLng(loc.path("lat").asDouble(), loc.path("lng").asDouble());
                }
            }
        } catch (Exception e) {
            log.error("Error calling Google Geocoding: {}", e.getMessage());
        }
        return null;
    }

    // =========================================================
    // PLACE SEARCH
    // =========================================================

    public List<MassExtractResult> searchPlaces(Long jobId, String query, String location,
                                                 double lat, double lng, int radiusKm, int maxResults) {
        // If real Google API key is available, use Google Places
        if (apiKey != null && !apiKey.isBlank() && !"mock".equalsIgnoreCase(apiKey)) {
            log.info("Using Google Places API for query: '{}' in '{}'", query, location);
            return searchPlacesViaGoogle(jobId, query, location, lat, lng, radiusKm, maxResults);
        }

        // Use OpenStreetMap Overpass API (free, real data, no API key needed)
        log.info("Using OpenStreetMap Overpass API for REAL business data: '{}' in '{}'", query, location);
        List<MassExtractResult> results = searchPlacesViaOverpass(jobId, query, location, lat, lng, radiusKm, maxResults);

        if (results.isEmpty()) {
            log.warn("Overpass returned 0 results. Trying broader search...");
            results = searchPlacesViaOverpassBroad(jobId, query, location, lat, lng, radiusKm * 2, maxResults);
        }

        return results;
    }

    // =========================================================
    // OVERPASS API - REAL DATA (OpenStreetMap)
    // =========================================================

    private List<MassExtractResult> searchPlacesViaOverpass(Long jobId, String query, String location,
                                                            double lat, double lng, int radiusKm, int maxResults) {
        String osmTags = mapQueryToOsmTags(query);
        int radiusMeters = radiusKm * 1000;

        // Build Overpass QL query using around filter for precise radius search
        String overpassQuery = String.format(
                "[out:json][timeout:30];\n" +
                "(\n" +
                "  node%s(around:%d,%f,%f);\n" +
                "  way%s(around:%d,%f,%f);\n" +
                "  relation%s(around:%d,%f,%f);\n" +
                ");\n" +
                "out body center %d;\n",
                osmTags, radiusMeters, lat, lng,
                osmTags, radiusMeters, lat, lng,
                osmTags, radiusMeters, lat, lng,
                maxResults
        );

        return executeOverpassQuery(jobId, query, location, overpassQuery, maxResults);
    }

    private List<MassExtractResult> searchPlacesViaOverpassBroad(Long jobId, String query, String location,
                                                                  double lat, double lng, int radiusKm, int maxResults) {
        int radiusMeters = radiusKm * 1000;
        String lowerQuery = query.toLowerCase().trim();

        // Broader search: use name matching with regex
        String overpassQuery = String.format(
                "[out:json][timeout:30];\n" +
                "(\n" +
                "  node[\"name\"~\"%s\",i](around:%d,%f,%f);\n" +
                "  way[\"name\"~\"%s\",i](around:%d,%f,%f);\n" +
                "  node[\"brand\"~\"%s\",i](around:%d,%f,%f);\n" +
                "  way[\"brand\"~\"%s\",i](around:%d,%f,%f);\n" +
                ");\n" +
                "out body center %d;\n",
                lowerQuery, radiusMeters, lat, lng,
                lowerQuery, radiusMeters, lat, lng,
                lowerQuery, radiusMeters, lat, lng,
                lowerQuery, radiusMeters, lat, lng,
                maxResults
        );

        return executeOverpassQuery(jobId, query, location, overpassQuery, maxResults);
    }

    private List<MassExtractResult> executeOverpassQuery(Long jobId, String query, String location,
                                                          String overpassQuery, int maxResults) {
        List<MassExtractResult> results = new ArrayList<>();

        try {
            log.info("Executing Overpass query for '{}' in '{}':\n{}", query, location, overpassQuery);

            RequestBody body = RequestBody.create(
                    "data=" + overpassQuery,
                    MediaType.parse("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                    .url("https://overpass-api.de/api/interpreter")
                    .post(body)
                    .header("User-Agent", "LeadHarvest/1.0 (business-scraper)")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Overpass API failed with HTTP {}", response.code());
                    return results;
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode elements = root.path("elements");

                if (!elements.isArray()) {
                    log.warn("Overpass returned no elements array");
                    return results;
                }

                log.info("Overpass returned {} raw elements", elements.size());

                for (JsonNode el : elements) {
                    if (results.size() >= maxResults) break;

                    MassExtractResult result = mapOverpassElement(jobId, query, location, el);
                    if (result != null && result.getName() != null && !result.getName().isBlank()) {
                        results.add(result);
                    }
                }

                log.info("Parsed {} valid business results from Overpass", results.size());
            }
        } catch (Exception e) {
            log.error("Error calling Overpass API: {}", e.getMessage(), e);
        }

        return results;
    }

    private MassExtractResult mapOverpassElement(Long jobId, String query, String location, JsonNode el) {
        JsonNode tags = el.path("tags");
        if (tags.isMissingNode() || !tags.has("name")) {
            return null;
        }

        String name = tags.path("name").asText("");
        if (name.isBlank()) return null;

        // Get coordinates - for ways/relations, use center
        double elLat;
        double elLng;
        if (el.has("center")) {
            elLat = el.path("center").path("lat").asDouble();
            elLng = el.path("center").path("lon").asDouble();
        } else {
            elLat = el.path("lat").asDouble();
            elLng = el.path("lon").asDouble();
        }

        // Extract all available OSM data
        String phone = extractPhone(tags);
        String website = extractWebsite(tags);
        String email = tags.path("email").asText(tags.path("contact:email").asText(null));
        String osmId = el.path("type").asText("node") + "/" + el.path("id").asLong();

        // Build address from OSM tags
        String address = buildOsmAddress(tags);
        String city = tags.path("addr:city").asText(
                tags.path("addr:suburb").asText(location));
        String state = tags.path("addr:state").asText("");
        String country = tags.path("addr:country").asText("");
        String postalCode = tags.path("addr:postcode").asText("");

        // Category from OSM amenity/shop/tourism tags
        String category = extractOsmCategory(tags, query);
        String subcategory = tags.path("cuisine").asText(
                tags.path("shop").asText(
                        tags.path("amenity").asText("")));

        // Description
        String description = tags.path("description").asText(
                tags.path("note").asText(""));

        // Opening hours
        String openingHours = tags.has("opening_hours") ? tags.path("opening_hours").asText() : null;

        // Brand info
        String brand = tags.path("brand").asText("");

        String displayName = name;
        if (!brand.isBlank() && !name.toLowerCase().contains(brand.toLowerCase())) {
            displayName = brand + " - " + name;
        }

        return MassExtractResult.builder()
                .jobId(jobId)
                .placeId(osmId)
                .name(displayName)
                .category(category)
                .subcategory(subcategory.isBlank() ? null : subcategory)
                .description(description.isBlank() ? null : description)
                .rating(null) // OSM doesn't have ratings
                .reviewsCount(0)
                .phone(phone)
                .email(email)
                .websiteUrl(website)
                .address(address.isBlank() ? null : address)
                .city(city.isBlank() ? location : city)
                .state(state.isBlank() ? null : state)
                .country(country.isBlank() ? null : country)
                .postalCode(postalCode.isBlank() ? null : postalCode)
                .latitude(elLat)
                .longitude(elLng)
                .mapsUrl("https://www.openstreetmap.org/" + osmId)
                .businessStatus("OPERATIONAL")
                .openingHours(openingHours != null ? "\"" + openingHours.replace("\"", "\\\"") + "\"" : null)
                .createdAt(Instant.now())
                .openNow(null)
                .build();
    }

    private String extractPhone(JsonNode tags) {
        if (tags.has("phone")) return tags.path("phone").asText();
        if (tags.has("contact:phone")) return tags.path("contact:phone").asText();
        if (tags.has("contact:mobile")) return tags.path("contact:mobile").asText();
        return null;
    }

    private String extractWebsite(JsonNode tags) {
        if (tags.has("website")) return tags.path("website").asText();
        if (tags.has("contact:website")) return tags.path("contact:website").asText();
        if (tags.has("url")) return tags.path("url").asText();
        if (tags.has("contact:facebook")) return tags.path("contact:facebook").asText();
        return null;
    }

    private String buildOsmAddress(JsonNode tags) {
        StringBuilder sb = new StringBuilder();
        String houseNum = tags.path("addr:housenumber").asText("");
        String street = tags.path("addr:street").asText("");
        String district = tags.path("addr:district").asText("");
        String city = tags.path("addr:city").asText("");
        String state = tags.path("addr:state").asText("");
        String postcode = tags.path("addr:postcode").asText("");

        if (!houseNum.isBlank()) sb.append(houseNum).append(", ");
        if (!street.isBlank()) sb.append(street).append(", ");
        if (!district.isBlank()) sb.append(district).append(", ");
        if (!city.isBlank()) sb.append(city);
        if (!state.isBlank()) sb.append(", ").append(state);
        if (!postcode.isBlank()) sb.append(" ").append(postcode);

        return sb.toString().trim().replaceAll(",\\s*$", "");
    }

    private String extractOsmCategory(JsonNode tags, String queryHint) {
        // Try to determine category from OSM tags
        String amenity = tags.path("amenity").asText("");
        String shop = tags.path("shop").asText("");
        String tourism = tags.path("tourism").asText("");
        String leisure = tags.path("leisure").asText("");
        String office = tags.path("office").asText("");
        String healthcare = tags.path("healthcare").asText("");

        if (!amenity.isBlank()) {
            return switch (amenity) {
                case "cafe" -> "Cafe";
                case "restaurant" -> "Restaurant";
                case "fast_food" -> "Fast Food";
                case "bar", "pub" -> "Bar & Pub";
                case "pharmacy" -> "Pharmacy";
                case "hospital", "clinic", "doctors" -> "Healthcare";
                case "dentist" -> "Dentist";
                case "school", "university", "college" -> "Education";
                case "bank" -> "Banking";
                case "fuel", "car_wash" -> "Automotive";
                case "place_of_worship" -> "Religious Place";
                case "library" -> "Library";
                case "cinema", "theatre" -> "Entertainment";
                default -> capitalizeFirst(amenity);
            };
        }
        if (!shop.isBlank()) {
            return switch (shop) {
                case "supermarket", "convenience" -> "Grocery & Retail";
                case "beauty", "hairdresser" -> "Beauty & Salon";
                case "clothes", "fashion" -> "Fashion & Apparel";
                case "electronics" -> "Electronics";
                case "bakery" -> "Bakery";
                case "jewelry" -> "Jewelry";
                case "optician" -> "Optician";
                case "mobile_phone" -> "Mobile & Electronics";
                default -> capitalizeFirst(shop);
            };
        }
        if (!tourism.isBlank()) {
            return switch (tourism) {
                case "hotel" -> "Hotel";
                case "guest_house" -> "Guest House";
                case "hostel" -> "Hostel";
                case "motel" -> "Motel";
                case "attraction" -> "Tourist Attraction";
                default -> capitalizeFirst(tourism);
            };
        }
        if (!leisure.isBlank()) {
            return switch (leisure) {
                case "fitness_centre", "sports_centre" -> "Gym & Fitness";
                case "swimming_pool" -> "Swimming Pool";
                case "park", "garden" -> "Park";
                default -> capitalizeFirst(leisure);
            };
        }
        if (!healthcare.isBlank()) return "Healthcare";
        if (!office.isBlank()) return "Office & Services";

        // Fallback to query hint
        return capitalizeFirst(queryHint);
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isBlank()) return "Local Business";
        return s.substring(0, 1).toUpperCase() + s.substring(1).replace("_", " ");
    }

    /**
     * Maps a user search query to OpenStreetMap tag filters for the Overpass API.
     */
    private String mapQueryToOsmTags(String query) {
        String lower = query.toLowerCase().trim();

        // Map common search queries to OSM amenity/shop/tourism/leisure tags
        if (lower.contains("cafe") || lower.contains("coffee")) {
            return "[\"amenity\"~\"cafe|coffee_shop\"]";
        }
        if (lower.contains("restaurant") || lower.contains("food") || lower.contains("dining")) {
            return "[\"amenity\"~\"restaurant|fast_food\"]";
        }
        if (lower.contains("hotel") || lower.contains("resort") || lower.contains("lodge")) {
            return "[\"tourism\"~\"hotel|guest_house|motel|hostel\"]";
        }
        if (lower.contains("gym") || lower.contains("fitness")) {
            return "[\"leisure\"~\"fitness_centre|sports_centre\"]";
        }
        if (lower.contains("school") || lower.contains("academy") || lower.contains("education")) {
            return "[\"amenity\"~\"school|university|college\"]";
        }
        if (lower.contains("hospital") || lower.contains("clinic") || lower.contains("medical")) {
            return "[\"amenity\"~\"hospital|clinic|doctors\"]";
        }
        if (lower.contains("salon") || lower.contains("beauty") || lower.contains("spa")) {
            return "[\"shop\"~\"beauty|hairdresser|massage\"]";
        }
        if (lower.contains("dentist") || lower.contains("dental")) {
            return "[\"amenity\"=\"dentist\"]";
        }
        if (lower.contains("real estate") || lower.contains("property")) {
            return "[\"office\"~\"estate_agent|property\"]";
        }
        if (lower.contains("software") || lower.contains("it ") || lower.contains("tech")) {
            return "[\"office\"~\"it|company|coworking\"]";
        }
        if (lower.contains("pharmacy") || lower.contains("chemist") || lower.contains("drug")) {
            return "[\"amenity\"~\"pharmacy\"][\"shop\"~\"chemist\"]";
        }
        if (lower.contains("bank") || lower.contains("atm")) {
            return "[\"amenity\"~\"bank|atm\"]";
        }
        if (lower.contains("bar") || lower.contains("pub") || lower.contains("brewery")) {
            return "[\"amenity\"~\"bar|pub|biergarten\"]";
        }
        if (lower.contains("bakery") || lower.contains("cake") || lower.contains("pastry")) {
            return "[\"shop\"=\"bakery\"]";
        }
        if (lower.contains("supermarket") || lower.contains("grocery") || lower.contains("store")) {
            return "[\"shop\"~\"supermarket|convenience|grocery\"]";
        }
        if (lower.contains("petrol") || lower.contains("gas") || lower.contains("fuel")) {
            return "[\"amenity\"=\"fuel\"]";
        }
        if (lower.contains("temple") || lower.contains("mosque") || lower.contains("church")) {
            return "[\"amenity\"=\"place_of_worship\"]";
        }
        if (lower.contains("park") || lower.contains("garden")) {
            return "[\"leisure\"~\"park|garden\"]";
        }
        if (lower.contains("cinema") || lower.contains("movie") || lower.contains("theatre")) {
            return "[\"amenity\"~\"cinema|theatre\"]";
        }
        if (lower.contains("laundry") || lower.contains("dry clean")) {
            return "[\"shop\"~\"laundry|dry_cleaning\"]";
        }
        if (lower.contains("tailor") || lower.contains("cloth")) {
            return "[\"shop\"~\"clothes|tailor|fashion\"]";
        }
        if (lower.contains("jewel") || lower.contains("gold")) {
            return "[\"shop\"~\"jewelry|jewellery\"]";
        }
        if (lower.contains("auto") || lower.contains("car") || lower.contains("mechanic")) {
            return "[\"shop\"~\"car|car_repair|car_parts\"]";
        }

        // Generic fallback: search by name
        return "[\"name\"~\"" + lower.replace("\"", "") + "\",i]";
    }

    // =========================================================
    // GOOGLE PLACES API (when real API key is provided)
    // =========================================================

    private List<MassExtractResult> searchPlacesViaGoogle(Long jobId, String query, String location,
                                                          double lat, double lng, int radiusKm, int maxResults) {
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
                        if (results.size() >= maxResults) break;
                        String placeId = placeNode.path("place_id").asText();
                        MassExtractResult details = getPlaceDetails(jobId, placeId);
                        if (details != null) {
                            results.add(details);
                        } else {
                            results.add(mapSearchResultToEntity(jobId, placeNode));
                        }
                    }

                    pageToken = root.path("next_page_token").asText(null);
                    if (pageToken == null || pageToken.isBlank()) break;
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
                boolean isLocality = false, isState = false, isCountry = false, isPostal = false;
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

        if (node.has("opening_hours")) {
            result.setOpeningHours(node.path("opening_hours").toString());
            result.setOpenNow(node.path("opening_hours").path("open_now").asBoolean(false));
        }

        if (node.has("editorial_summary")) {
            result.setDescription(node.path("editorial_summary").path("overview").asText());
        }

        if (node.has("photos")) {
            result.setPhotos(node.path("photos").toString());
            if (node.path("photos").size() > 0) {
                result.setLogoUrl(node.path("photos").get(0).path("photo_reference").asText());
            }
        }

        return result;
    }
}
