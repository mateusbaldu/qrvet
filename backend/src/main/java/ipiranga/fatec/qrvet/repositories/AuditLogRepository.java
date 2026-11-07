package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.dtos.response.AuditActionSummary;
import ipiranga.fatec.qrvet.dtos.response.AuditLogReportRow;
import ipiranga.fatec.qrvet.dtos.response.AuditResourceSummary;
import ipiranga.fatec.qrvet.models.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Query("""
            SELECT a.action AS action, COUNT(a) AS total
            FROM AuditLog a
            WHERE (:startDate IS NULL OR a.timestamp >= :startDate)
              AND (:endDate IS NULL OR a.timestamp <= :endDate)
              AND (:userId IS NULL OR a.userId = :userId)
              AND (:actionPattern IS NULL OR LOWER(a.action) LIKE :actionPattern)
              AND (:resourcePattern IS NULL OR LOWER(a.resourceName) LIKE :resourcePattern)
            GROUP BY a.action
            ORDER BY COUNT(a) DESC, a.action ASC
            """)
    List<AuditActionSummary> countByAction(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("userId") Long userId,
            @Param("actionPattern") String actionPattern,
            @Param("resourcePattern") String resourcePattern);

    @Query("""
            SELECT a.resourceName AS resourceName, COUNT(a) AS total
            FROM AuditLog a
            WHERE (:startDate IS NULL OR a.timestamp >= :startDate)
              AND (:endDate IS NULL OR a.timestamp <= :endDate)
              AND (:userId IS NULL OR a.userId = :userId)
              AND (:actionPattern IS NULL OR LOWER(a.action) LIKE :actionPattern)
              AND (:resourcePattern IS NULL OR LOWER(a.resourceName) LIKE :resourcePattern)
            GROUP BY a.resourceName
            ORDER BY COUNT(a) DESC, a.resourceName ASC
            """)
    List<AuditResourceSummary> countByResource(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("userId") Long userId,
            @Param("actionPattern") String actionPattern,
            @Param("resourcePattern") String resourcePattern);

    @Query("""
            SELECT a.id AS id,
                   a.timestamp AS timestamp,
                   a.userId AS userId,
                   a.userEmail AS userEmail,
                   a.ipAddress AS ipAddress,
                   a.action AS action,
                   a.resourceName AS resourceName,
                   a.resourceId AS resourceId,
                   a.details AS details
            FROM AuditLog a
            WHERE (:startDate IS NULL OR a.timestamp >= :startDate)
              AND (:endDate IS NULL OR a.timestamp <= :endDate)
              AND (:userId IS NULL OR a.userId = :userId)
              AND (:actionPattern IS NULL OR LOWER(a.action) LIKE :actionPattern)
              AND (:resourcePattern IS NULL OR LOWER(a.resourceName) LIKE :resourcePattern)
            ORDER BY a.id DESC
            """)
    List<AuditLogReportRow> findReportRows(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("userId") Long userId,
            @Param("actionPattern") String actionPattern,
            @Param("resourcePattern") String resourcePattern);
}
