package com.example.demo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_records")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private InspectionTask task;

    private String productName;
    private Boolean isCompliant;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String extractedAttributesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String violationsJson;

    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditRecord() {
    }

    public AuditRecord(InspectionTask task, String productName, Boolean isCompliant, String extractedAttributesJson, String violationsJson) {
        this.task = task;
        this.productName = productName;
        this.isCompliant = isCompliant;
        this.extractedAttributesJson = extractedAttributesJson;
        this.violationsJson = violationsJson;
    }

    public Long getId() {
        return id;
    }

    public InspectionTask getTask() {
        return task;
    }

    public String getProductName() {
        return productName;
    }

    public Boolean getIsCompliant() {
        return isCompliant;
    }

    public String getExtractedAttributesJson() {
        return extractedAttributesJson;
    }

    public String getViolationsJson() {
        return violationsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
