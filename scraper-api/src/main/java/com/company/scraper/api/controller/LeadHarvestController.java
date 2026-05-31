package com.company.scraper.api.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/lead-harvest", produces = MediaType.APPLICATION_JSON_VALUE)
public class LeadHarvestController {

    @GetMapping("/dashboard")
    public LeadHarvestDashboard dashboard() {
        return LeadHarvestDashboard.sample();
    }

    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisBatch analyze(@RequestBody AnalyzeRequest request) {
        List<String> urls = request.urls();
        List<AnalysisResult> results = new ArrayList<>();
        for (String rawUrl : urls) {
            String normalizedUrl = normalizeUrl(rawUrl);
            results.add(analyzeUrl(normalizedUrl));
        }
        return new AnalysisBatch(results.size(), results);
    }

    private AnalysisResult analyzeUrl(String url) {
        String domain = URI.create(url).getHost() == null ? url : URI.create(url).getHost().replaceFirst("^www\\.", "");
        String category = classify(domain);
        String businessName = toBusinessName(domain);
        int contactCompleteness = score(domain, 92, 6);
        int seoScore = score(domain, 84, 8);
        int presenceScore = score(domain, 78, 10);
        int websiteQualityScore = score(domain, 88, 7);
        int leadScore = Math.min(99, Math.round((contactCompleteness + seoScore + presenceScore + websiteQualityScore) / 4.0f));

        List<InsightItem> businessInfo = List.of(
            new InsightItem("Business Name", businessName, 98),
            new InsightItem("Category", category, 91),
            new InsightItem("Website", url, 100),
            new InsightItem("Business Type", inferBusinessType(category), 84)
        );

        List<InsightItem> contacts = List.of(
            new InsightItem("Phone", "+1-555-010-" + Math.abs(domain.hashCode() % 1000), 72),
            new InsightItem("Email", "info@" + domain, 86),
            new InsightItem("Contact Form", url.replaceAll("/$", "") + "/contact", 93)
        );

        List<InsightItem> socialProfiles = List.of(
            new InsightItem("LinkedIn", "https://www.linkedin.com/company/" + slug(domain), 78),
            new InsightItem("Instagram", "https://www.instagram.com/" + slug(domain), 64),
            new InsightItem("Facebook", "https://www.facebook.com/" + slug(domain), 67)
        );

        List<InsightItem> services = List.of(
            new InsightItem("Core Service", category + " consulting", 81),
            new InsightItem("Premium Package", "Growth plan", 75),
            new InsightItem("Discovery Call", "Book a strategy session", 89)
        );

        List<InsightItem> team = List.of(
            new InsightItem("Founder", businessName + " Team", 71),
            new InsightItem("CEO", businessName + " Leadership", 66),
            new InsightItem("Operations", "Customer success", 62)
        );

        return new AnalysisResult(
            url,
            businessName,
            category,
            leadScore,
            websiteQualityScore,
            presenceScore,
            seoScore,
            contactCompleteness,
            "Hot",
            businessInfo,
            contacts,
            socialProfiles,
            services,
            team,
            List.of("Home", "About", "Services", "Contact", "Pricing", "Blog"),
            Map.of(
                "cssSelector", "body",
                "xpath", "/html/body",
                "confidence", 94,
                "sourcePage", url
            ),
            List.of(
                new ConfidenceItem("Business Name", 98),
                new ConfidenceItem("Phone", 91),
                new ConfidenceItem("Email", 88),
                new ConfidenceItem("Address", 84),
                new ConfidenceItem("Social Links", 90)
            )
        );
    }

    private static String classify(String domain) {
        String normalized = domain.toLowerCase(Locale.ROOT);
        if (normalized.contains("dent") || normalized.contains("clinic") || normalized.contains("medical")) {
            return "Healthcare";
        }
        if (normalized.contains("restaurant") || normalized.contains("cafe") || normalized.contains("food")) {
            return "Hospitality";
        }
        if (normalized.contains("law") || normalized.contains("legal")) {
            return "Legal Services";
        }
        if (normalized.contains("shop") || normalized.contains("store") || normalized.contains("commerce")) {
            return "E-Commerce";
        }
        if (normalized.contains("agency") || normalized.contains("studio") || normalized.contains("marketing")) {
            return "Agency";
        }
        return "Business Services";
    }

    private static String inferBusinessType(String category) {
        return switch (category) {
            case "Healthcare" -> "Practice";
            case "Hospitality" -> "Venue";
            case "Legal Services" -> "Firm";
            case "E-Commerce" -> "Store";
            case "Agency" -> "Service Business";
            default -> "Company";
        };
    }

    private static String toBusinessName(String domain) {
        String base = domain.replaceFirst("\\.[^.]+$", "");
        String[] parts = base.split("[.-]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.append(" Group").toString();
    }

    private static String slug(String domain) {
        return domain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static String normalizeUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            return "https://example.com";
        }
        if (!trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private static int score(String seed, int base, int spread) {
        int adjustment = Math.abs(seed.hashCode()) % Math.max(1, spread);
        return Math.min(99, base + adjustment);
    }

    public record AnalyzeRequest(List<String> urls) {
    }

    public record AnalysisBatch(int processedCount, List<AnalysisResult> results) {
    }

    public record AnalysisResult(
        String url,
        String businessName,
        String category,
        int leadScore,
        int websiteQualityScore,
        int digitalPresenceScore,
        int seoScore,
        int contactCompletenessScore,
        String leadTier,
        List<InsightItem> businessInfo,
        List<InsightItem> contacts,
        List<InsightItem> socialProfiles,
        List<InsightItem> services,
        List<InsightItem> teamMembers,
        List<String> pageDiscovery,
        Map<String, Object> smartInspector,
        List<ConfidenceItem> confidence
    ) {
    }

    public record InsightItem(String label, String value, int confidence) {
    }

    public record ConfidenceItem(String field, int score) {
    }

    public record LeadHarvestDashboard(
        String platformName,
        String subtitle,
        LocalDate generatedOn,
        OverviewMetrics metrics,
        List<String> modules,
        List<PipelineStage> crmPipeline,
        List<HistoryColumn> historyColumns,
        List<InsightItem> topInsights,
        List<DomainTrend> industryTrends,
        List<DomainTrend> geographyTrends
    ) {
        static LeadHarvestDashboard sample() {
            return new LeadHarvestDashboard(
                "Lead Harvest",
                "Enterprise website intelligence, lead generation, CRM, and visual extraction studio",
                LocalDate.now(),
                new OverviewMetrics(12842, 8731, 12994, 9487, 6881, 3928, 94.2, 91.5, 88.7),
                List.of(
                    "Overview",
                    "Visual Studio",
                    "Lead Intelligence",
                    "Analytics",
                    "CRM Pipeline",
                    "History Grid",
                    "Import / Export"
                ),
                List.of(
                    new PipelineStage("New", 142),
                    new PipelineStage("Qualified", 118),
                    new PipelineStage("Contacted", 84),
                    new PipelineStage("Meeting Scheduled", 41),
                    new PipelineStage("Proposal Sent", 19),
                    new PipelineStage("Won", 13),
                    new PipelineStage("Lost", 22)
                ),
                List.of(
                    new HistoryColumn("Website URL", true),
                    new HistoryColumn("Business Name", true),
                    new HistoryColumn("Category", true),
                    new HistoryColumn("Phone", true),
                    new HistoryColumn("Email", true),
                    new HistoryColumn("Address", true),
                    new HistoryColumn("Website Available", true),
                    new HistoryColumn("Social Profiles", true),
                    new HistoryColumn("Services", true),
                    new HistoryColumn("Lead Score", true),
                    new HistoryColumn("Date Added", true),
                    new HistoryColumn("Status", true)
                ),
                List.of(
                    new InsightItem("Hot lead coverage", "78% of analyzed domains are targetable", 92),
                    new InsightItem("Contact completeness", "Average score above 88", 91),
                    new InsightItem("Social presence", "LinkedIn is the dominant profile", 84)
                ),
                List.of(
                    new DomainTrend("Healthcare", 31),
                    new DomainTrend("E-Commerce", 22),
                    new DomainTrend("Agency", 18),
                    new DomainTrend("Hospitality", 15),
                    new DomainTrend("Legal Services", 14)
                ),
                List.of(
                    new DomainTrend("North America", 44),
                    new DomainTrend("Europe", 23),
                    new DomainTrend("Asia Pacific", 18),
                    new DomainTrend("Middle East", 9),
                    new DomainTrend("Other", 6)
                )
            );
        }
    }

    public record OverviewMetrics(
        int totalUrlsProcessed,
        int businessesFound,
        int contactsFound,
        int emailsFound,
        int phonesFound,
        int socialProfilesFound,
        double websiteQualityScore,
        double digitalPresenceScore,
        double opportunityScore
    ) {
    }

    public record PipelineStage(String name, int count) {
    }

    public record HistoryColumn(String name, boolean visible) {
    }

    public record DomainTrend(String label, int value) {
    }
}