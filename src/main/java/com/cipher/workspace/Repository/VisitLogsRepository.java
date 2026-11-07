package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.VisitLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VisitLogsRepository extends JpaRepository<VisitLogs, UUID>, JpaSpecificationExecutor<VisitLogs> {

    // Active visits (in-space visitors)
    long countByCheckOutTimeIsNull();

    List<VisitLogs> findByCheckOutTimeIsNullOrderByCheckInTimeDesc();

    Page<VisitLogs> findByCheckOutTimeIsNullOrderByCheckInTimeDesc(Pageable pageable);

    // All visits ordered by check-in time descending
    List<VisitLogs> findAllByOrderByCheckInTimeDesc();

    Page<VisitLogs> findAllByOrderByCheckInTimeDesc(Pageable pageable);

    // Customer visit histories
    List<VisitLogs> findByCustomerIdOrderByCheckInTimeDesc(UUID customerId);

    List<VisitLogs> findByCustomerIdOrderByCheckInTimeAsc(UUID customerId);

    Page<VisitLogs> findByCustomerIdOrderByCheckInTimeDesc(UUID customerId, Pageable pageable);

    long countByCustomerId(UUID customerId);

    long countByCustomerIdAndCheckInTimeBetween(UUID customerId, LocalDateTime start, LocalDateTime end);

    boolean existsByCustomerId(UUID customerId);

    // Date range queries
    List<VisitLogs> findByCheckInTimeBetweenOrderByCheckInTimeAsc(LocalDateTime start, LocalDateTime end);

    List<VisitLogs> findByCheckInTimeBetweenOrderByCheckInTimeDesc(LocalDateTime start, LocalDateTime end);

    long countByCheckInTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByCheckInTimeBetweenAndCheckOutTimeIsNull(LocalDateTime start, LocalDateTime end);

    long countByCheckInTimeBetweenAndCheckOutTimeIsNotNull(LocalDateTime start, LocalDateTime end);

    // Financial revenue aggregations
    @Query("SELECT COALESCE(SUM(v.amountPaid), 0) FROM VisitLogs v")
    BigDecimal sumAllAmountPaid();

    @Query("SELECT COALESCE(SUM(v.amountPaid), 0) FROM VisitLogs v WHERE v.checkInTime BETWEEN :start AND :end")
    BigDecimal sumAmountPaidBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT v.serviceType, COALESCE(SUM(v.amountPaid), 0) FROM VisitLogs v WHERE v.checkInTime BETWEEN :start AND :end GROUP BY v.serviceType")
    List<Object[]> sumAmountPaidByServiceTypeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(v.amountPaid), 0) FROM VisitLogs v WHERE v.customer.id = :customerId")
    BigDecimal sumAmountPaidByCustomerId(@Param("customerId") UUID customerId);

    // User reference check (audit safe staff deactivation)
    @Query("""
        SELECT COUNT(v) > 0 FROM VisitLogs v
        WHERE (v.user IS NOT NULL AND v.user.id = :userId)
           OR (v.checkedOutBy IS NOT NULL AND v.checkedOutBy.id = :userId)
           OR (v.updatedBy IS NOT NULL AND v.updatedBy.id = :userId)
    """)
    boolean existsByAnyUserReference(@Param("userId") UUID userId);

    // Filtered visits helper methods using specification
    default Page<VisitLogs> findFilteredVisits(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            UUID customerId,
            String status,
            Pageable pageable
    ) {
        Specification<VisitLogs> spec = VisitLogsSpecification.filterVisits(
                startDateTime,
                endDateTime,
                customerId,
                status
        );
        Pageable sortedPageable = (pageable != null && pageable.getSort().isSorted())
                ? pageable
                : PageRequest.of(
                        pageable != null ? pageable.getPageNumber() : 0,
                        pageable != null ? pageable.getPageSize() : 10,
                        VisitLogsSpecification.sortByCheckInTimeDesc()
                );
        return findAll(spec, sortedPageable);
    }

    default List<VisitLogs> findFilteredVisitsList(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            UUID customerId,
            String status
    ) {
        Specification<VisitLogs> spec = VisitLogsSpecification.filterVisits(
                startDateTime,
                endDateTime,
                customerId,
                status
        );
        return findAll(spec, VisitLogsSpecification.sortByCheckInTimeDesc());
    }
}
