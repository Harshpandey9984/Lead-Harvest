package com.company.scraper.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
            <!doctype html>
            <html lang=\"en\">
            <head>
              <meta charset=\"utf-8\" />
              <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
              <title>Scraper API</title>
              <style>
                body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; margin: 32px; }
                code, pre { background: #f6f8fa; padding: 2px 6px; border-radius: 4px; }
                pre { padding: 12px; overflow: auto; }
                .grid { display: grid; grid-template-columns: 1fr; gap: 16px; max-width: 980px; }
                a { color: #0969da; text-decoration: none; }
                a:hover { text-decoration: underline; }
              </style>
            </head>
            <body>
              <h1>Scraper API is running</h1>
              <div class=\"grid\">
                <div>
                  <h2>Health & diagnostics</h2>
                  <ul>
                    <li><a href=\"/actuator/health\">/actuator/health</a></li>
                    <li><a href=\"/actuator/info\">/actuator/info</a></li>
                    <li><a href=\"/actuator\">/actuator</a></li>
                  </ul>
                </div>

                <div>
                  <h2>API endpoints</h2>
                  <ul>
                    <li><code>POST /api/jobs</code></li>
                    <li><code>POST /api/jobs/{jobId}/targets</code></li>
                    <li><code>GET  /api/jobs/{jobId}/targets</code></li>
                    <li><code>POST /api/jobs/{jobId}/trigger</code></li>
                    <li><code>GET  /api/results/targets/{targetId}</code></li>
                    <li><code>POST /api/proxies</code></li>
                    <li><code>GET  /api/proxies</code></li>
                    <li><code>POST /api/notifications</code></li>
                  </ul>
                </div>

                <div>
                  <h2>Authentication</h2>
                  <p>
                    If you set <code>API_KEY</code> (or <code>security.api-key</code>), requests to <code>/api/**</code> must include
                    <code>X-API-Key</code>.
                  </p>
                  <pre>curl.exe -H "X-API-Key: YOUR_KEY" http://localhost:8080/api/proxies</pre>
                </div>
              </div>
            </body>
            </html>
            """;
    }
}
