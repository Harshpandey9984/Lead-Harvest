package com.company.scraper.api.service;

import com.company.scraper.common.dto.MassExtractResultResponse;
import com.company.scraper.common.model.MassExtractContact;
import com.company.scraper.common.model.MassExtractResult;
import com.company.scraper.common.model.MassExtractSocial;
import com.company.scraper.common.repository.MassExtractContactRepository;
import com.company.scraper.common.repository.MassExtractResultRepository;
import com.company.scraper.common.repository.MassExtractSocialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MassExportService {

    private final MassExtractResultRepository resultRepository;
    private final MassExtractSocialRepository socialRepository;
    private final MassExtractContactRepository contactRepository;
    private final ObjectMapper objectMapper;

    public List<MassExtractResultResponse> getResultResponses(Long jobId) {
        List<MassExtractResult> results = resultRepository.findByJobId(jobId);
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> resultIds = results.stream().map(MassExtractResult::getId).toList();
        List<MassExtractSocial> socials = socialRepository.findByResultIdIn(resultIds);
        List<MassExtractContact> contacts = contactRepository.findByResultIdIn(resultIds);

        Map<Long, List<String>> socialsMap = new HashMap<>();
        for (MassExtractSocial s : socials) {
            socialsMap.computeIfAbsent(s.getResultId(), k -> new ArrayList<>()).add(s.getUrl());
        }

        Map<Long, List<String>> emailsMap = new HashMap<>();
        Map<Long, List<String>> phonesMap = new HashMap<>();
        for (MassExtractContact c : contacts) {
            if ("EMAIL".equalsIgnoreCase(c.getType())) {
                emailsMap.computeIfAbsent(c.getResultId(), k -> new ArrayList<>()).add(c.getValue());
            } else if ("PHONE".equalsIgnoreCase(c.getType())) {
                phonesMap.computeIfAbsent(c.getResultId(), k -> new ArrayList<>()).add(c.getValue());
            }
        }

        List<MassExtractResultResponse> responses = new ArrayList<>();
        for (MassExtractResult r : results) {
            responses.add(MassExtractResultResponse.builder()
                    .id(r.getId())
                    .jobId(r.getJobId())
                    .placeId(r.getPlaceId())
                    .name(r.getName())
                    .category(r.getCategory())
                    .subcategory(r.getSubcategory())
                    .description(r.getDescription())
                    .rating(r.getRating())
                    .reviewsCount(r.getReviewsCount())
                    .phone(r.getPhone())
                    .secondaryPhone(r.getSecondaryPhone())
                    .email(r.getEmail())
                    .websiteUrl(r.getWebsiteUrl())
                    .address(r.getAddress())
                    .city(r.getCity())
                    .state(r.getState())
                    .country(r.getCountry())
                    .postalCode(r.getPostalCode())
                    .latitude(r.getLatitude())
                    .longitude(r.getLongitude())
                    .mapsUrl(r.getMapsUrl())
                    .priceLevel(r.getPriceLevel())
                    .businessStatus(r.getBusinessStatus())
                    .openingHours(r.getOpeningHours())
                    .openNow(r.getOpenNow())
                    .permanentlyClosed(r.getPermanentlyClosed())
                    .logoUrl(r.getLogoUrl())
                    .photos(r.getPhotos())
                    .reviewsSummary(r.getReviewsSummary())
                    .reviewsKeywords(r.getReviewsKeywords())
                    .socialLinks(socialsMap.getOrDefault(r.getId(), Collections.emptyList()))
                    .scrapedEmails(emailsMap.getOrDefault(r.getId(), Collections.emptyList()))
                    .scrapedPhones(phonesMap.getOrDefault(r.getId(), Collections.emptyList()))
                    .build());
        }
        return responses;
    }

    public byte[] exportToExcel(Long jobId) throws IOException {
        List<MassExtractResultResponse> data = getResultResponses(jobId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Leads");
            sheet.createFreezePane(0, 1);

            // Style for headers: bold, forest green background, white text
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            String[] headers = {
                    "Name", "Category", "Subcategory", "Rating", "Reviews Count",
                    "Primary Phone", "Website URL", "Primary Email", "Address", "City",
                    "State", "Country", "Postal Code", "Maps URL", "Price Level",
                    "Business Status", "Social Links (Comma Separated)", "All Scraped Emails", "All Scraped Phones"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (MassExtractResultResponse r : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getName());
                row.createCell(1).setCellValue(r.getCategory() != null ? r.getCategory() : "");
                row.createCell(2).setCellValue(r.getSubcategory() != null ? r.getSubcategory() : "");
                
                Cell ratingCell = row.createCell(3);
                if (r.getRating() != null) {
                    ratingCell.setCellValue(r.getRating());
                }
                
                Cell reviewsCell = row.createCell(4);
                if (r.getReviewsCount() != null) {
                    reviewsCell.setCellValue(r.getReviewsCount());
                }

                row.createCell(5).setCellValue(r.getPhone() != null ? r.getPhone() : "");
                row.createCell(6).setCellValue(r.getWebsiteUrl() != null ? r.getWebsiteUrl() : "");
                row.createCell(7).setCellValue(r.getEmail() != null ? r.getEmail() : "");
                row.createCell(8).setCellValue(r.getAddress() != null ? r.getAddress() : "");
                row.createCell(9).setCellValue(r.getCity() != null ? r.getCity() : "");
                row.createCell(10).setCellValue(r.getState() != null ? r.getState() : "");
                row.createCell(11).setCellValue(r.getCountry() != null ? r.getCountry() : "");
                row.createCell(12).setCellValue(r.getPostalCode() != null ? r.getPostalCode() : "");
                row.createCell(13).setCellValue(r.getMapsUrl() != null ? r.getMapsUrl() : "");

                Cell priceCell = row.createCell(14);
                if (r.getPriceLevel() != null) {
                    priceCell.setCellValue(r.getPriceLevel());
                }

                row.createCell(15).setCellValue(r.getBusinessStatus() != null ? r.getBusinessStatus() : "");
                row.createCell(16).setCellValue(String.join(", ", r.getSocialLinks()));
                row.createCell(17).setCellValue(String.join(", ", r.getScrapedEmails()));
                row.createCell(18).setCellValue(String.join(", ", r.getScrapedPhones()));
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    public byte[] exportToCsv(Long jobId) {
        List<MassExtractResultResponse> data = getResultResponses(jobId);
        StringBuilder sb = new StringBuilder();
        
        // Append Header
        sb.append("Name,Category,Subcategory,Rating,Reviews Count,Primary Phone,Website URL,Primary Email,Address,City,State,Country,Postal Code,Maps URL,Price Level,BusinessStatus,Socials,AllEmails,AllPhones\n");

        for (MassExtractResultResponse r : data) {
            sb.append(escapeCsv(r.getName())).append(",")
              .append(escapeCsv(r.getCategory())).append(",")
              .append(escapeCsv(r.getSubcategory())).append(",")
              .append(r.getRating() != null ? r.getRating() : "").append(",")
              .append(r.getReviewsCount() != null ? r.getReviewsCount() : "").append(",")
              .append(escapeCsv(r.getPhone())).append(",")
              .append(escapeCsv(r.getWebsiteUrl())).append(",")
              .append(escapeCsv(r.getEmail())).append(",")
              .append(escapeCsv(r.getAddress())).append(",")
              .append(escapeCsv(r.getCity())).append(",")
              .append(escapeCsv(r.getState())).append(",")
              .append(escapeCsv(r.getCountry())).append(",")
              .append(escapeCsv(r.getPostalCode())).append(",")
              .append(escapeCsv(r.getMapsUrl())).append(",")
              .append(r.getPriceLevel() != null ? r.getPriceLevel() : "").append(",")
              .append(escapeCsv(r.getBusinessStatus())).append(",")
              .append(escapeCsv(String.join("; ", r.getSocialLinks()))).append(",")
              .append(escapeCsv(String.join("; ", r.getScrapedEmails()))).append(",")
              .append(escapeCsv(String.join("; ", r.getScrapedPhones()))).append("\n");
        }

        return sb.toString().getBytes();
    }

    public byte[] exportToJson(Long jobId) throws IOException {
        List<MassExtractResultResponse> data = getResultResponses(jobId);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
