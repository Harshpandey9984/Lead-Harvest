package com.company.scraper.api;

import com.company.scraper.api.service.GooglePlacesService;
import com.company.scraper.api.service.MassExportService;
import com.company.scraper.common.model.MassExtractResult;
import com.company.scraper.common.repository.MassExtractContactRepository;
import com.company.scraper.common.repository.MassExtractResultRepository;
import com.company.scraper.common.repository.MassExtractSocialRepository;
import com.company.scraper.common.dto.MassExtractResultResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class MassExtractJobTest {

    @Test
    public void testMockGeocoding() {
        OkHttpClient client = Mockito.mock(OkHttpClient.class);
        ObjectMapper mapper = new ObjectMapper();
        GooglePlacesService service = new GooglePlacesService(client, mapper);

        // Geocoding with empty key should use mock geocoding and return default coordinates (Bhopal)
        GooglePlacesService.LatLng latLng = service.geocode("Bhopal");
        assertNotNull(latLng);
        assertEquals(23.259933, latLng.lat, 0.001);
        assertEquals(77.412615, latLng.lng, 0.001);
    }

    @Test
    public void testMockPlacesSearch() {
        OkHttpClient client = Mockito.mock(OkHttpClient.class);
        ObjectMapper mapper = new ObjectMapper();
        GooglePlacesService service = new GooglePlacesService(client, mapper);

        // Search with empty key should trigger mock search and return generated mock places
        List<MassExtractResult> results = service.searchPlaces(1L, "Cafe", "Bhopal", 23.259933, 77.412615, 10, 5);
        assertNotNull(results);
        assertEquals(5, results.size());
        
        for (MassExtractResult res : results) {
            assertEquals(1L, res.getJobId());
            assertEquals("Cafe", res.getCategory());
            assertTrue(res.getName().contains("Cafe"));
            assertTrue(res.getWebsiteUrl().contains("http://"));
            assertNotNull(res.getPhone());
            assertNotNull(res.getCity());
        }
    }

    @Test
    public void testExportGeneration() throws IOException {
        MassExtractResultRepository resultRepository = Mockito.mock(MassExtractResultRepository.class);
        MassExtractSocialRepository socialRepository = Mockito.mock(MassExtractSocialRepository.class);
        MassExtractContactRepository contactRepository = Mockito.mock(MassExtractContactRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        MassExportService exportService = new MassExportService(resultRepository, socialRepository, contactRepository, mapper);

        MassExtractResult mockResult = MassExtractResult.builder()
                .id(101L)
                .jobId(1L)
                .placeId("ChIJ_mock_1")
                .name("Cafe Hub 1")
                .category("Cafe")
                .phone("+91 755 5123456")
                .websiteUrl("http://www.cafe-hub-1.com")
                .address("12, Arera Colony, Bhopal, MP, India")
                .city("Bhopal")
                .rating(4.5)
                .reviewsCount(120)
                .build();

        when(resultRepository.findByJobId(1L)).thenReturn(Collections.singletonList(mockResult));
        when(socialRepository.findByResultIdIn(Mockito.anyList())).thenReturn(Collections.emptyList());
        when(contactRepository.findByResultIdIn(Mockito.anyList())).thenReturn(Collections.emptyList());

        // Test DTO list mapping
        List<MassExtractResultResponse> responses = exportService.getResultResponses(1L);
        assertEquals(1, responses.size());
        assertEquals("Cafe Hub 1", responses.get(0).getName());

        // Test Excel Export
        byte[] excelBytes = exportService.exportToExcel(1L);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        // Test CSV Export
        byte[] csvBytes = exportService.exportToCsv(1L);
        assertNotNull(csvBytes);
        String csv = new String(csvBytes);
        assertTrue(csv.contains("Cafe Hub 1"));
        assertTrue(csv.contains("Bhopal"));

        // Test JSON Export
        byte[] jsonBytes = exportService.exportToJson(1L);
        assertNotNull(jsonBytes);
        assertTrue(jsonBytes.length > 0);
    }
}
