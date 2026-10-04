package com.govtrack360.repository;

import com.govtrack360.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:search IS NULL OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:userId IS NULL OR a.user.id = :userId) " +
           "AND (:module IS NULL OR a.module = :module) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:from IS NULL OR a.timestamp >= :from) " +
           "AND (:to IS NULL OR a.timestamp <= :to) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> search(@Param("search") String search,
                           @Param("userId") Long userId,
                           @Param("module") String module,
                           @Param("action") String action,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);
}
