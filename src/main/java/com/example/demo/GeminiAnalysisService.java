package com.example.demo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiAnalysisService {

    @Value("${X-goog-api-key}")
    private String apiKey;

    private static final int MAX_RETRIES_PER_ENDPOINT = 3;
    private static final long INITIAL_BACKOFF_MS = 1500;

    public String analyzeUploadedImage(String fileName) throws IOException, InterruptedException {
        String cleanApiKey = apiKey.trim().replaceAll("^[\\\"']|[\\\"']$", "");
        if (cleanApiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is missing");
        }

        Path imagePath = Paths.get("uploads", fileName);
        if (!Files.exists(imagePath)) {
            throw new IOException("File not found in uploads directory: " + imagePath.toAbsolutePath());
        }

        byte[] imageBytes = Files.readAllBytes(imagePath);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String mimeType = Files.probeContentType(imagePath);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }

        List<String> modelEndpoints = List.of(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=" + cleanApiKey,
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=" + cleanApiKey,
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + cleanApiKey
        );

        IOException lastException = null;
        for (String endpointUrl : modelEndpoints) {
            try {
                return sendWithRetry(endpointUrl, base64Image, mimeType);
            } catch (IOException e) {
                System.err.println("Endpoint failed or overloaded (" + endpointUrl + "). Attempting next model fallback...");
                lastException = e;
            }
        }

        throw (lastException != null) ? lastException : new IOException("All Gemini model endpoints failed.");
    }

    private String sendWithRetry(String apiUrl, String base64Image, String mimeType) throws IOException, InterruptedException {

        String prompt = "You are a Legal Metrology (Packaged Commodities) Rules, 2011 auditor (SIH 26034). "
                + "Examine the product packaging image and verify compliance against official rules. "
                + "Extract data into strict JSON with the following schema: "
                + "{"
                + "  \"mrp\": {\"value\": string/null, \"is_valid_format\": boolean, \"rule_ref\": \"Rule 2(m) & Rule 6(1)(e)\"}, "
                + "  \"net_quantity\": {\"value\": string/null, \"is_valid_unit\": boolean, \"rule_ref\": \"Rule 6(1)(c) & Rule 13\"}, "
                + "  \"month_year\": {\"value\": string/null, \"rule_ref\": \"Rule 6(1)(d)\"}, "
                + "  \"generic_name\": {\"value\": string/null, \"rule_ref\": \"Rule 6(1)(b)\"}, "
                + "  \"manufacturer_details\": {\"value\": string/null, \"has_full_address\": boolean, \"rule_ref\": \"Rule 6(1)(a) & Rule 10\"}, "
                + "  \"consumer_care\": {\"value\": string/null, \"has_phone\": boolean, \"has_email\": boolean, \"rule_ref\": \"Rule 6(2)\"}, "
                + "  \"is_compliant\": boolean, "
                + "  \"violations\": [string] "
                + "} "
                + "Rules to enforce: "
                + "1. MRP format must contain 'incl. of all taxes' or 'inclusive of all taxes'. "
                + "2. Net Quantity unit must be standard SI (g, kg, ml, l, N, U). Expressions like 'approx' or 'min' make it non-compliant. "
                + "3. Manufacturer details must include complete address. "
                + "4. Consumer care must include phone or email. "
                + "Return ONLY raw JSON.";

        String escapedPrompt = prompt.replace("\"", "\\\"");

        String jsonPayload = """
        {
          "contents": [
            {
              "parts": [
                { "text": "%s" },
                {
                  "inline_data": {
                    "mime_type": "%s",
                    "data": "%s"
                  }
                }
              ]
            }
          ],
          "generationConfig": {
            "response_mime_type": "application/json",
            "temperature": 0.1
          }
        }
        """.formatted(escapedPrompt, mimeType, base64Image);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        long backoff = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES_PER_ENDPOINT; attempt++) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode == 200) {
                return response.body();
            }

            if (statusCode == 503 || statusCode == 429) {
                System.out.printf("Gemini API returned HTTP %d (Attempt %d/%d). Retrying in %d ms...\n",
                        statusCode, attempt, MAX_RETRIES_PER_ENDPOINT, backoff);

                if (attempt < MAX_RETRIES_PER_ENDPOINT) {
                    Thread.sleep(backoff);
                    backoff *= 2;
                    continue;
                }
            }

            System.err.println("--- GEMINI ERROR LOG ---");
            System.err.println("Status: " + statusCode);
            System.err.println("Response: " + response.body());
            System.err.println("------------------------");
            throw new IOException("Gemini API Error (" + statusCode + "): " + response.body());
        }

        throw new IOException("Gemini endpoint " + apiUrl + " exceeded maximum retry attempts.");
    }
}
