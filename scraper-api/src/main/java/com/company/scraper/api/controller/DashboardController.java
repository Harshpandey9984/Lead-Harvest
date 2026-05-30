package com.company.scraper.api.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Serves the frontend dashboard
 */
@Controller
public class DashboardController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/api/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ApiStatus getStatus() {
        return new ApiStatus(
            "OK",
            "Web Scraper API is running",
            "1.0.0",
            "All services operational"
        );
    }

    public static class ApiStatus {
        public String status;
        public String message;
        public String version;
        public String systemStatus;

        public ApiStatus(String status, String message, String version, String systemStatus) {
            this.status = status;
            this.message = message;
            this.version = version;
            this.systemStatus = systemStatus;
        }
    }
}
