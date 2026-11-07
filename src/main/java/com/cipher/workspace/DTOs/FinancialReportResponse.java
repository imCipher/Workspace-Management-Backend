package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.FinancialStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record FinancialReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalRevenue,
        BigDecimal totalExpenses,
        BigDecimal netProfitOrLoss,
        FinancialStatus status,
        long totalVisitsCount,
        long totalExpensesCount,
        Map<String, BigDecimal> revenueByServiceType,
        Map<String, BigDecimal> expensesByCategory,
        LocalDateTime generatedAt,
        UUID generatedByStaffId,
        String generatedByStaffName
) {
    public FinancialReportResponse(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalRevenue,
            BigDecimal totalExpenses,
            BigDecimal netProfitOrLoss,
            String status,
            long totalVisitsCount,
            long totalExpensesCount,
            Map<String, BigDecimal> revenueByServiceType,
            Map<String, BigDecimal> expensesByCategory,
            LocalDateTime generatedAt,
            UUID generatedByStaffId,
            String generatedByStaffName
    ) {
        this(
                startDate,
                endDate,
                totalRevenue,
                totalExpenses,
                netProfitOrLoss,
                status != null ? FinancialStatus.valueOf(status.trim().toUpperCase()) : null,
                totalVisitsCount,
                totalExpensesCount,
                revenueByServiceType,
                expensesByCategory,
                generatedAt,
                generatedByStaffId,
                generatedByStaffName
        );
    }
}
