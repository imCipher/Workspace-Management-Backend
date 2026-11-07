package com.cipher.workspace.DTOs;

import java.util.List;

public record CustomerAnalyticsResponse(
        long totalCustomersCount,
        long regularCustomersCount,
        long occasionalCustomersCount,
        int regularThreshold,
        double regularPercentage,
        int year,
        int month,
        List<CustomerVisitAnalyticsDto> customers
) {
}
