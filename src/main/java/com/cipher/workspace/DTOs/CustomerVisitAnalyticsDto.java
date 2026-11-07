package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerVisitAnalyticsDto(
        UUID customerId,
        String customerName,
        String phoneNumber,
        int totalVisits,
        int monthlyVisits,
        boolean isRegular,
        String customerTag,
        BigDecimal totalSpent,
        LocalDateTime firstVisitDate,
        LocalDateTime lastVisitDate,
        String preferredServiceType
) {
    public CustomerVisitAnalyticsDto(
            UUID customerId,
            String customerName,
            String phoneNumber,
            int totalVisits,
            int monthlyVisits,
            boolean isRegular,
            String customerTag,
            BigDecimal totalSpent,
            LocalDateTime firstVisitDate,
            LocalDateTime lastVisitDate,
            ServiceType preferredServiceType
    ) {
        this(
                customerId,
                customerName,
                phoneNumber,
                totalVisits,
                monthlyVisits,
                isRegular,
                customerTag,
                totalSpent,
                firstVisitDate,
                lastVisitDate,
                preferredServiceType != null ? preferredServiceType.name() : null
        );
    }
}
