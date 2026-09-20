package com.example.demo;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String category;
    private Double latitude;
    private Double longitude;
    private Double baselineRiskScore;
    private Integer totalViolationsCount = 0;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL)
    private List<InspectionTask> inspectionTasks;

    public Store() {
    }

    public Store(String name, String category, Double latitude, Double longitude, Double baselineRiskScore) {
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.baselineRiskScore = baselineRiskScore;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getBaselineRiskScore() {
        return baselineRiskScore;
    }

    public void setBaselineRiskScore(Double baselineRiskScore) {
        this.baselineRiskScore = baselineRiskScore;
    }

    public Integer getTotalViolationsCount() {
        return totalViolationsCount;
    }

    public void setTotalViolationsCount(Integer totalViolationsCount) {
        this.totalViolationsCount = totalViolationsCount;
    }
}
