package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByTaskId(Long taskId);

    List<AuditRecord> findByTaskStoreId(Long storeId);
}
