package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.VisitLogs;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Singular alias for {@link VisitLogsSpecification}.
 */
public final class VisitLogSpecification {

    private VisitLogSpecification() {
    }

    public static Specification<VisitLogs> withStartDate(LocalDateTime startDate) {
        return VisitLogsSpecification.withStartDate(startDate);
    }

    public static Specification<VisitLogs> withStartDate(LocalDate startDate) {
        return VisitLogsSpecification.withStartDate(startDate);
    }

    public static Specification<VisitLogs> withEndDate(LocalDateTime endDate) {
        return VisitLogsSpecification.withEndDate(endDate);
    }

    public static Specification<VisitLogs> withEndDate(LocalDate endDate) {
        return VisitLogsSpecification.withEndDate(endDate);
    }

    public static Specification<VisitLogs> withCustomerId(UUID customerId) {
        return VisitLogsSpecification.withCustomerId(customerId);
    }

    public static Specification<VisitLogs> withStatus(String status) {
        return VisitLogsSpecification.withStatus(status);
    }

    public static Sort sortByCheckInTimeDesc() {
        return VisitLogsSpecification.sortByCheckInTimeDesc();
    }

    public static Specification<VisitLogs> filterVisits(
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID customerId,
            String status
    ) {
        return VisitLogsSpecification.filterVisits(startDate, endDate, customerId, status);
    }

    public static Specification<VisitLogs> filterVisits(
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID customerId,
            String status,
            String search,
            String serviceType,
            String paymentType
    ) {
        return VisitLogsSpecification.filterVisits(startDate, endDate, customerId, status, search, serviceType, paymentType);
    }
}
