package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionTaskRepository extends JpaRepository<InspectionTask, Long> {

    List<InspectionTask> findByInspectorId(Long inspectorId);

    List<InspectionTask> findByInspectorIdAndStatus(Long inspectorId, String status);

    List<InspectionTask> findByStatus(String status);
}
