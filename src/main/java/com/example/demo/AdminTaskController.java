package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminTaskController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InspectionTaskRepository taskRepository;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @Autowired
    private StoreRepository storeRepository;

    @GetMapping("/inspectors")
    public ResponseEntity<List<User>> getInspectors() {
        List<User> inspectors = userRepository.findByRole("inspector");
        return ResponseEntity.ok(inspectors);
    }

    @GetMapping("/stores")
    public ResponseEntity<List<Store>> getAllStores() {
        List<Store> stores = storeRepository.findAll();
        return ResponseEntity.ok(stores);
    }

    private void saveAuditRecordsForTask(InspectionTask task, String reportJson) {
        if (reportJson == null || reportJson.isBlank()) {
            return;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(reportJson);

            List<com.fasterxml.jackson.databind.JsonNode> reportNodes = new java.util.ArrayList<>();
            if (rootNode.isArray()) {
                rootNode.forEach(reportNodes::add);
            } else if (rootNode.isObject()) {
                reportNodes.add(rootNode);
            }

            for (int i = 0; i < reportNodes.size(); i++) {
                com.fasterxml.jackson.databind.JsonNode node = reportNodes.get(i);

                boolean isCompliant = node.has("is_compliant") && node.get("is_compliant").asBoolean();

                String productName = "Audited Product #" + (i + 1);
                if (node.has("generic_name") && node.get("generic_name").has("value") && !node.get("generic_name").get("value").isNull()) {
                    productName = node.get("generic_name").get("value").asText();
                }

                String violationsJson = node.has("violations") ? node.get("violations").toString() : "[]";
                String extractedAttributesJson = node.toString();

                AuditRecord record = new AuditRecord(
                        task,
                        productName,
                        isCompliant,
                        extractedAttributesJson,
                        violationsJson
                );

                auditRecordRepository.save(record);
            }
        } catch (Exception e) {
            System.err.println("Failed to insert AuditRecord entities: " + e.getMessage());
        }
    }

    @PostMapping("/assign-task")
    public ResponseEntity<?> assignTask(@RequestBody TaskRequest request) {
        User inspector = userRepository.findById(request.getInspectorId()).orElse(null);
        Store store = storeRepository.findByName(request.getLocationName()).orElse(null);

        InspectionTask task = new InspectionTask(
                request.getLocationName(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRiskScore(),
                inspector
        );

        task.setStatus("REQUESTED");

        if (store != null) {
            task.setStore(store);
        }

        taskRepository.save(task);
        return ResponseEntity.ok("{\"message\": \"Task requested successfully and sent to inspector!\"}");
    }

    @PostMapping("/tasks/accept/{taskId}")
    public ResponseEntity<?> acceptTask(@PathVariable Long taskId) {
        Optional<InspectionTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Task not found\"}");
        }

        InspectionTask task = taskOpt.get();
        task.setStatus("ASSIGNED");

        User inspector = task.getInspector();
        if (inspector != null) {
            inspector.setStatus("ON_TASK");
            userRepository.save(inspector);
        }

        taskRepository.save(task);
        return ResponseEntity.ok("{\"message\": \"Task request accepted!\"}");
    }

    @PostMapping("/tasks/decline/{taskId}")
    public ResponseEntity<?> declineTask(@PathVariable Long taskId) {
        Optional<InspectionTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Task not found\"}");
        }

        InspectionTask task = taskOpt.get();
        task.setStatus("DECLINED");

        User inspector = task.getInspector();
        if (inspector != null) {
            inspector.setStatus("AVAILABLE");
            userRepository.save(inspector);
        }

        taskRepository.save(task);
        return ResponseEntity.ok("{\"message\": \"Task request declined.\"}");
    }

    @PostMapping("/tasks/complete/{taskIdStr}")
    public ResponseEntity<?> completeTaskOrAdhoc(
            @PathVariable String taskIdStr,
            @RequestBody(required = false) Map<String, Object> body) {

        String newReportJson = (body != null && body.containsKey("reportData")) ? body.get("reportData").toString() : "[]";

        if ("adhoc".equalsIgnoreCase(taskIdStr) || "0".equals(taskIdStr)) {
            Long inspectorId = body != null && body.containsKey("inspectorId")
                    ? Long.parseLong(body.get("inspectorId").toString()) : 37L;

            User inspector = userRepository.findById(inspectorId).orElse(null);

            String locationName = (body != null && body.containsKey("locationName"))
                    ? body.get("locationName").toString()
                    : "Self-Initiated On-Site Audit";

            Store store = storeRepository.findByName(locationName).orElse(null);

            InspectionTask adhocTask = new InspectionTask(
                    locationName,
                    19.0760, 72.8777,
                    0.50,
                    inspector
            );
            adhocTask.setStatus("COMPLETED");
            adhocTask.setReportData(newReportJson);

            if (store != null) {
                adhocTask.setStore(store);
                if (newReportJson != null && newReportJson.contains("\"is_compliant\":false")) {
                    int currentViolations = store.getTotalViolationsCount() != null ? store.getTotalViolationsCount() : 0;
                    store.setTotalViolationsCount(currentViolations + 1);

                    double currentRisk = store.getBaselineRiskScore() != null ? store.getBaselineRiskScore() : 0.50;
                    store.setBaselineRiskScore(Math.min(1.00, currentRisk + 0.15));

                    storeRepository.save(store);
                }
            }

            InspectionTask savedTask = taskRepository.save(adhocTask);
            saveAuditRecordsForTask(savedTask, newReportJson);

            return ResponseEntity.ok("{\"message\": \"Ad-hoc inspection report submitted successfully!\"}");
        }

        try {
            Long taskId = Long.parseLong(taskIdStr);
            Optional<InspectionTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("{\"error\": \"Task not found\"}");
            }

            InspectionTask task = taskOpt.get();
            task.setStatus("COMPLETED");

            String existingData = task.getReportData();
            if (existingData != null && !existingData.isBlank() && existingData.startsWith("[")) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<Object> existingList = mapper.readValue(existingData, List.class);
                    List<Object> newList = mapper.readValue(newReportJson, List.class);
                    existingList.addAll(newList);
                    task.setReportData(mapper.writeValueAsString(existingList));
                } catch (Exception e) {
                    task.setReportData(newReportJson);
                }
            } else {
                task.setReportData(newReportJson);
            }

            Store store = task.getStore();
            if (store == null && task.getLocationName() != null) {
                store = storeRepository.findByName(task.getLocationName()).orElse(null);
                if (store != null) {
                    task.setStore(store);
                }
            }

            if (store != null && newReportJson != null && newReportJson.contains("\"is_compliant\":false")) {
                int currentViolations = store.getTotalViolationsCount() != null ? store.getTotalViolationsCount() : 0;
                store.setTotalViolationsCount(currentViolations + 1);

                double currentRisk = store.getBaselineRiskScore() != null ? store.getBaselineRiskScore() : 0.50;
                store.setBaselineRiskScore(Math.min(1.00, currentRisk + 0.15));

                storeRepository.save(store);
            }

            InspectionTask savedTask = taskRepository.save(task);
            saveAuditRecordsForTask(savedTask, newReportJson);

            User inspector = task.getInspector();
            if (inspector != null) {
                inspector.setStatus("AVAILABLE");
                userRepository.save(inspector);
            }

            return ResponseEntity.ok("{\"message\": \"Report submitted and task completed successfully!\"}");
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"Invalid task ID format\"}");
        }
    }

    @GetMapping("/tasks/all")
    public ResponseEntity<List<InspectionTask>> getAllTasks() {
        List<InspectionTask> tasks = taskRepository.findAll();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/inspector/{inspectorId}")
    public ResponseEntity<List<InspectionTask>> getTasksByInspector(@PathVariable Long inspectorId) {
        List<InspectionTask> tasks = taskRepository.findAll();
        List<InspectionTask> filtered = tasks.stream()
                .filter(t -> t.getInspector() != null && t.getInspector().getId().equals(inspectorId))
                .filter(t -> "REQUESTED".equalsIgnoreCase(t.getStatus()) || "ASSIGNED".equalsIgnoreCase(t.getStatus()))
                .toList();
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/tasks/completed")
    public ResponseEntity<List<InspectionTask>> getCompletedTasks() {
        List<InspectionTask> completedTasks = taskRepository.findByStatus("COMPLETED");
        return ResponseEntity.ok(completedTasks);
    }

    @PostMapping("/register-inspector")
    public ResponseEntity<?> registerInspector(@RequestBody InspectorRegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Username is required\"}");
        }

        Double lat = request.getLatitude() != null ? request.getLatitude() : 19.0760;
        Double lng = request.getLongitude() != null ? request.getLongitude() : 72.8777;

        User newInspector = new User(
                request.getUsername(),
                "password",
                "inspector",
                lat,
                lng,
                "AVAILABLE"
        );

        userRepository.save(newInspector);
        return ResponseEntity.ok("{\"message\": \"Inspector registered successfully!\"}");
    }

    @GetMapping("/heatmap-data")
    public ResponseEntity<?> getHeatmapData() {
        List<Store> stores = storeRepository.findAll();

        if (stores.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<double[]> heatPoints = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newHttpClient();

            List<Map<String, Object>> recordsList = new ArrayList<>();
            for (Store s : stores) {
                if (isValidCoordinate(s.getLatitude(), s.getLongitude())) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("latitude", s.getLatitude());
                    record.put("longitude", s.getLongitude());
                    record.put("totalViolationsCount", s.getTotalViolationsCount() != null ? s.getTotalViolationsCount() : 0);
                    record.put("baselineRiskScore", s.getBaselineRiskScore() != null ? s.getBaselineRiskScore() : 0.50);
                    record.put("months_since_inspection", 6);
                    record.put("merchant_density", 10);
                    recordsList.add(record);
                }
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(recordsList);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/predict-heatmaps"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(response.body());

            if (rootNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : rootNode) {
                    if (node.isArray() && node.size() >= 2) {
                        double lat = node.get(0).asDouble();
                        double lng = node.get(1).asDouble();
                        double risk = node.size() >= 3 ? node.get(2).asDouble() : 0.5;
                        heatPoints.add(new double[]{lat, lng, risk});
                    } else if (node.isObject()) {
                        double lat = node.has("latitude") ? node.get("latitude").asDouble() : (node.has("lat") ? node.get("lat").asDouble() : 0.0);
                        double lng = node.has("longitude") ? node.get("longitude").asDouble() : (node.has("lng") ? node.get("lng").asDouble() : 0.0);
                        double risk = node.has("predicted_risk") ? node.get("predicted_risk").asDouble() : (node.has("risk") ? node.get("risk").asDouble() : 0.5);
                        if (isValidCoordinate(lat, lng)) {
                            heatPoints.add(new double[]{lat, lng, risk});
                        }
                    }
                }
            }

            if (!heatPoints.isEmpty()) {
                return ResponseEntity.ok(heatPoints);
            }

        } catch (Exception e) {
            System.err.println("FastAPI risk prediction model unavailable, utilizing DB store baseline risk scores: " + e.getMessage());
        }

        for (Store s : stores) {
            if (isValidCoordinate(s.getLatitude(), s.getLongitude())) {
                Double risk = s.getBaselineRiskScore() != null ? s.getBaselineRiskScore() : 0.50;
                heatPoints.add(new double[]{s.getLatitude(), s.getLongitude(), risk});
            }
        }
        return ResponseEntity.ok(heatPoints);
    }

    private boolean isValidCoordinate(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    public static class InspectorRegisterRequest {

        private String username;
        private Double latitude;
        private Double longitude;

        public String getUsername() {
            return username;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }
    }

    public static class TaskRequest {

        private Long inspectorId;
        private String locationName;
        private Double latitude;
        private Double longitude;
        private Double riskScore;

        public Long getInspectorId() {
            return inspectorId;
        }

        public String getLocationName() {
            return locationName;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public Double getRiskScore() {
            return riskScore;
        }
    }
}
