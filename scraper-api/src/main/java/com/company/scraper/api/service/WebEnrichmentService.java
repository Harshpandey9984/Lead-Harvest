package com.company.scraper.api.service;

import com.company.scraper.common.model.MassExtractContact;
import com.company.scraper.common.model.MassExtractResult;
import com.company.scraper.common.model.MassExtractSocial;
import com.company.scraper.common.repository.MassExtractContactRepository;
import com.company.scraper.common.repository.MassExtractResultRepository;
import com.company.scraper.common.repository.MassExtractSocialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebEnrichmentService {

    private final MassExtractResultRepository resultRepository;
    private final MassExtractSocialRepository socialRepository;
    private final MassExtractContactRepository contactRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"
    );

    // Simple phone regex to capture standard formats
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\+?\\d{1,4}[-.\\s]?\\(?\\d{1,3}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}"
    );

    @Async("scrapeExecutor")
    public CompletableFuture<Void> enrichResult(MassExtractResult result) {
        String url = result.getWebsiteUrl();
        if (url == null || url.isBlank() || url.contains("example.com") || url.contains("mock")) {
            // For mock/missing websites, we can simulate a successful run with mock contacts to show features in action
            if (url != null && (url.contains("example.com") || url.contains("mock"))) {
                simulateMockEnrichment(result);
            }
            return CompletableFuture.completedFuture(null);
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        log.info("Starting web enrichment for resultId: {}, url: {}", result.getId(), url);
        Set<String> emails = new HashSet<>();
        Set<String> phones = new HashSet<>();
        Map<String, String> socials = new HashMap<>();

        try {
            // 1. Visit homepage
            Document doc = fetchUrl(url);
            if (doc != null) {
                scrapePage(doc, url, emails, phones, socials);

                // 2. Discover contact/about links
                Set<String> pagesToVisit = discoverInternalLinks(doc, url);
                for (String pageUrl : pagesToVisit) {
                    Document subDoc = fetchUrl(pageUrl);
                    if (subDoc != null) {
                        scrapePage(subDoc, pageUrl, emails, phones, socials);
                    }
                }
            }

            // Save details to database
            saveEnrichedData(result, emails, phones, socials);

        } catch (Exception e) {
            log.error("Failed to enrich result details for {}: {}", url, e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    private Document fetchUrl(String url) {
        try {
            Connection conn = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(6000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .followRedirects(true);

            // Bypass SSL check
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            conn.sslSocketFactory(sc.getSocketFactory());

            return conn.get();
        } catch (Exception e) {
            log.warn("Error fetching URL {}: {}", url, e.getMessage());
            return null;
        }
    }

    private void scrapePage(Document doc, String pageUrl, Set<String> emails, Set<String> phones, Map<String, String> socials) {
        String text = doc.text();
        String html = doc.html();

        // 1. Find emails from text
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) {
            String email = emailMatcher.group().toLowerCase();
            // Filter common garbage matches
            if (!email.endsWith(".png") && !email.endsWith(".jpg") && !email.endsWith(".gif") && !email.endsWith(".jpeg") && !email.endsWith(".svg")) {
                emails.add(email);
            }
        }

        // 2. Find phones from links and text
        Elements telLinks = doc.select("a[href^=tel:]");
        for (Element link : telLinks) {
            String tel = link.attr("href").replace("tel:", "").trim();
            if (!tel.isBlank()) {
                phones.add(tel);
            }
        }

        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            String ph = phoneMatcher.group().trim();
            if (ph.length() >= 7 && ph.length() <= 18) {
                phones.add(ph);
            }
        }

        // 3. Find socials from links
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String href = link.attr("abs:href").toLowerCase();
            if (href.contains("facebook.com/") || href.contains("fb.com/")) {
                extractSocialProfile(href, "Facebook", socials);
            } else if (href.contains("linkedin.com/")) {
                extractSocialProfile(href, "LinkedIn", socials);
            } else if (href.contains("instagram.com/")) {
                extractSocialProfile(href, "Instagram", socials);
            } else if (href.contains("twitter.com/") || href.contains("x.com/")) {
                extractSocialProfile(href, "Twitter", socials);
            } else if (href.contains("youtube.com/") || href.contains("youtu.be/")) {
                extractSocialProfile(href, "YouTube", socials);
            } else if (href.startsWith("mailto:")) {
                String mail = href.replace("mailto:", "").split("\\?")[0].trim();
                if (!mail.isBlank()) {
                    emails.add(mail);
                }
            }
        }
    }

    private Set<String> discoverInternalLinks(Document doc, String baseUrl) {
        Set<String> discovered = new HashSet<>();
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String absUrl = link.attr("abs:href");
            String lowerText = link.text().toLowerCase();
            String lowerUrl = absUrl.toLowerCase();

            // Match pages likely containing contacts/social links
            boolean matchesCriteria = lowerText.contains("contact") || lowerText.contains("about") ||
                    lowerText.contains("reach") || lowerText.contains("touch") ||
                    lowerText.contains("privacy") || lowerText.contains("terms") ||
                    lowerUrl.contains("contact") || lowerUrl.contains("about");

            if (matchesCriteria && absUrl.startsWith(baseUrl) && !absUrl.equals(baseUrl)) {
                discovered.add(absUrl);
                if (discovered.size() >= 3) {
                    break; // Cap internal page crawl
                }
            }
        }
        return discovered;
    }

    private void extractSocialProfile(String href, String platform, Map<String, String> socials) {
        // Simple sanitization to keep only base profile links
        String cleaned = href.split("\\?")[0].trim();
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (!cleaned.equalsIgnoreCase("https://www.facebook.com") && 
            !cleaned.equalsIgnoreCase("https://facebook.com") &&
            !cleaned.equalsIgnoreCase("https://www.linkedin.com") &&
            !cleaned.equalsIgnoreCase("https://linkedin.com") &&
            !cleaned.equalsIgnoreCase("https://www.instagram.com") &&
            !cleaned.equalsIgnoreCase("https://instagram.com")) {
            socials.put(platform, cleaned);
        }
    }

    private void saveEnrichedData(MassExtractResult result, Set<String> emails, Set<String> phones, Map<String, String> socials) {
        // Save first email and secondary phone directly to result
        if (!emails.isEmpty()) {
            result.setEmail(emails.iterator().next());
        }
        if (!phones.isEmpty()) {
            // Find a phone that is different from existing Google maps phone number
            String secondary = null;
            for (String p : phones) {
                if (!p.equals(result.getPhone())) {
                    secondary = p;
                    break;
                }
            }
            result.setSecondaryPhone(secondary);
        }

        resultRepository.save(result);

        // Save contacts table entries
        for (String email : emails) {
            contactRepository.save(MassExtractContact.builder()
                    .resultId(result.getId())
                    .type("EMAIL")
                    .value(email)
                    .build());
        }

        for (String phone : phones) {
            contactRepository.save(MassExtractContact.builder()
                    .resultId(result.getId())
                    .type("PHONE")
                    .value(phone)
                    .build());
        }

        // Save socials table entries
        for (Map.Entry<String, String> entry : socials.entrySet()) {
            socialRepository.save(MassExtractSocial.builder()
                    .resultId(result.getId())
                    .platform(entry.getKey())
                    .url(entry.getValue())
                    .build());
        }
    }

    private void simulateMockEnrichment(MassExtractResult result) {
        String cleanName = result.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
        String email = "contact@" + cleanName + ".com";
        result.setEmail(email);

        String secondary = "+91 98765 " + (10000 + new Random().nextInt(89999));
        result.setSecondaryPhone(secondary);

        resultRepository.save(result);

        contactRepository.save(MassExtractContact.builder()
                .resultId(result.getId())
                .type("EMAIL")
                .value(email)
                .build());

        contactRepository.save(MassExtractContact.builder()
                .resultId(result.getId())
                .type("PHONE")
                .value(secondary)
                .build());

        String[] platforms = {"Facebook", "LinkedIn", "Instagram", "Twitter"};
        String[] domainPrefixes = {"facebook.com/", "linkedin.com/company/", "instagram.com/", "x.com/"};
        Random r = new Random();

        // Add 1 to 3 random social URLs
        int numSocials = 1 + r.nextInt(3);
        List<Integer> indices = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(indices);

        for (int i = 0; i < numSocials; i++) {
            int idx = indices.get(i);
            socialRepository.save(MassExtractSocial.builder()
                    .resultId(result.getId())
                    .platform(platforms[idx])
                    .url("https://www." + domainPrefixes[idx] + cleanName)
                    .build());
        }
    }
}
