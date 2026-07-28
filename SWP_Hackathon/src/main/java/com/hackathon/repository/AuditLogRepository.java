package com.hackathon.repository;

import com.hackathon.entity.AuditLog;
import com.hackathon.entity.enums.AuditEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query(
            value = """
                    SELECT COUNT(a) 
                    FROM AuditLog a
                    WHERE a.createdAt >= :from
                    """
    )
    long countTotalLogs24h(@Param("from") LocalDateTime from);
    List<AuditLog> findTop10ByOrderByCreatedAtDesc();
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            AuditEntityType entityType,
            Integer entityId);
}
