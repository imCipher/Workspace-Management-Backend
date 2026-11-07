package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.VisitLogs;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VisitLogsSpecification {

    public static Specification<VisitLogs> withStartDate(LocalDateTime startDate) {
        return (root, query, cb) -> startDate == null ? null : cb.greaterThanOrEqualTo(root.get("checkInTime"), startDate);
    }

    public static Specification<VisitLogs> withStartDate(LocalDate startDate) {
        return withStartDate(startDate != null ? startDate.atStartOfDay() : null);
    }

    public static Specification<VisitLogs> withEndDate(LocalDateTime endDate) {
        return (root, query, cb) -> endDate == null ? null : cb.lessThanOrEqualTo(root.get("checkInTime"), endDate);
    }

    public static Specification<VisitLogs> withEndDate(LocalDate endDate) {
        return withEndDate(endDate != null ? endDate.atTime(LocalTime.MAX) : null);
    }

    public static Specification<VisitLogs> withCustomerId(UUID customerId) {
        return (root, query, cb) -> customerId == null ? null : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<VisitLogs> withStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return null;
            }
            String trimmed = status.trim();
            if ("ACTIVE".equalsIgnoreCase(trimmed)) {
                return cb.isNull(root.get("checkOutTime"));
            } else if ("COMPLETED".equalsIgnoreCase(trimmed)) {
                return cb.isNotNull(root.get("checkOutTime"));
            }
            return null;
        };
    }

    public static Sort sortByCheckInTimeDesc() {
        return Sort.by(Sort.Direction.DESC, "checkInTime");
    }

    public static Specification<VisitLogs> filterVisits(
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID customerId,
            String status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInTime"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkInTime"), endDate));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (status != null && !status.isBlank()) {
                String trimmed = status.trim();
                if ("ACTIVE".equalsIgnoreCase(trimmed)) {
                    predicates.add(cb.isNull(root.get("checkOutTime")));
                } else if ("COMPLETED".equalsIgnoreCase(trimmed)) {
                    predicates.add(cb.isNotNull(root.get("checkOutTime")));
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
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
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInTime"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkInTime"), endDate));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (status != null && !status.isBlank()) {
                String trimmed = status.trim();
                if ("ACTIVE".equalsIgnoreCase(trimmed)) {
                    predicates.add(cb.isNull(root.get("checkOutTime")));
                } else if ("COMPLETED".equalsIgnoreCase(trimmed)) {
                    predicates.add(cb.isNotNull(root.get("checkOutTime")));
                }
            }
            if (serviceType != null && !serviceType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("serviceType")), serviceType.trim().toUpperCase()));
            }
            if (paymentType != null && !paymentType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("paymentType")), paymentType.trim().toUpperCase()));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("customer").get("fullname")), pattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("customer").get("phoneNumber")), pattern);
                predicates.add(cb.or(nameMatch, phoneMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
