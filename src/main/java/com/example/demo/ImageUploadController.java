package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "*")
public class ImageUploadController {

    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private GeminiAnalysisService geminiAnalysisService;

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> acceptImages(
            @RequestParam(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestParam(value = "images[]", required = false) List<MultipartFile> bracketImageFiles) {

        List<MultipartFile> files = new ArrayList<>();
        if (imageFiles != null) {
            files.addAll(imageFiles);
        }
        if (bracketImageFiles != null) {
            files.addAll(bracketImageFiles);
        }

        if (files.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"No images received.\"}");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            MultipartFile file = files.get(0);
            String rawFilename = file.getOriginalFilename();

            if (rawFilename != null && !file.isEmpty()) {
                String sanitizedFilename = System.currentTimeMillis() + "_" + rawFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");

                Path destinationPath = uploadPath.resolve(sanitizedFilename);
                Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Saved file successfully: " + sanitizedFilename);

                String analysisResultJson = geminiAnalysisService.analyzeUploadedImage(sanitizedFilename);

                return ResponseEntity.ok(analysisResultJson);
            }

            return ResponseEntity.badRequest().body("{\"error\": \"File is empty or invalid.\"}");

        } catch (IOException e) {
            e.printStackTrace();
            String safeMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'").replace("\n", " ") : "IO Error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"File save failed: " + safeMsg + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            String safeMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'").replace("\n", " ") : "Analysis Error";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Gemini analysis failed: " + safeMsg + "\"}");
        }
    }
}
