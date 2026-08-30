package com.mishraachandan.booking_system.repository;

import com.mishraachandan.booking_system.dto.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUserIdOrEntityType(Long userId, String entityType, Pageable pageable);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
