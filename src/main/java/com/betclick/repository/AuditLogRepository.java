package com.betclick.repository;

import com.betclick.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByChangedAtDesc();
    List<AuditLog> findAllByOrderByChangedAtDesc(Pageable pageable);
    List<AuditLog> findByTableNameOrderByChangedAtDesc(String tableName);
    List<AuditLog> findByTableNameOrderByChangedAtDesc(String tableName, Pageable pageable);
}
