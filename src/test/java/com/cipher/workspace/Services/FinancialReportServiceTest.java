package com.cipher.workspace.Services;

import com.cipher.workspace.DTOs.FinancialReportResponse;
import com.cipher.workspace.Entity.Expenses;
import com.cipher.workspace.Entity.FinancialStatus;
import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Entity.VisitLogs;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Repository.ExpensesRepository;
import com.cipher.workspace.Repository.VisitLogsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceTest {

    @Mock
    private VisitLogsRepository visitLogsRepository;

    @Mock
    private ExpensesRepository expensesRepository;

    private FinancialReportService financialReportService;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        financialReportService = new FinancialReportService(visitLogsRepository, expensesRepository);

        testAdmin = new User();
        testAdmin.setId(UUID.randomUUID());
        testAdmin.setUsername("admin_finance");
        testAdmin.setFullname("Finance Admin");
        testAdmin.setRole(Role.ADMIN);

        var auth = new UsernamePasswordAuthenticationToken(testAdmin, null, testAdmin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateReport_withProfit_calculatesCorrectly() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        VisitLogs v1 = VisitLogs.builder()
                .amountPaid(new BigDecimal("50000.00"))
                .serviceType("SHARED_WORKSPACE")
                .build();

        VisitLogs v2 = VisitLogs.builder()
                .amountPaid(new BigDecimal("30000.00"))
                .serviceType("PRIVATE_OFFICE")
                .build();

        when(visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(any(), any()))
                .thenReturn(List.of(v1, v2));

        Expenses e1 = Expenses.builder()
                .amount(new BigDecimal("20000.00"))
                .category("UTILITIES")
                .build();

        Expenses e2 = Expenses.builder()
                .amount(new BigDecimal("15000.00"))
                .category("MAINTENANCE")
                .build();

        when(expensesRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(any(), any()))
                .thenReturn(List.of(e1, e2));

        FinancialReportResponse report = financialReportService.generateReport(start, end);

        assertNotNull(report);
        assertEquals(start, report.startDate());
        assertEquals(end, report.endDate());
        assertEquals(new BigDecimal("80000.00"), report.totalRevenue());
        assertEquals(new BigDecimal("35000.00"), report.totalExpenses());
        assertEquals(new BigDecimal("45000.00"), report.netProfitOrLoss());
        assertEquals(FinancialStatus.PROFIT, report.status());
        assertEquals(2, report.totalVisitsCount());
        assertEquals(2, report.totalExpensesCount());

        // Breakdown assertions
        assertEquals(new BigDecimal("50000.00"), report.revenueByServiceType().get("SHARED_WORKSPACE"));
        assertEquals(new BigDecimal("30000.00"), report.revenueByServiceType().get("PRIVATE_OFFICE"));
        assertEquals(new BigDecimal("20000.00"), report.expensesByCategory().get("UTILITIES"));
        assertEquals(new BigDecimal("15000.00"), report.expensesByCategory().get("MAINTENANCE"));

        // Staff accountability
        assertEquals(testAdmin.getId(), report.generatedByStaffId());
        assertEquals("Finance Admin", report.generatedByStaffName());
    }

    @Test
    void generateReport_withLoss_calculatesCorrectly() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        VisitLogs v1 = VisitLogs.builder()
                .amountPaid(new BigDecimal("10000.00"))
                .serviceType("DEDICATED_DESK")
                .build();

        when(visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(any(), any()))
                .thenReturn(List.of(v1));

        Expenses e1 = Expenses.builder()
                .amount(new BigDecimal("30000.00"))
                .category("PAYROLL")
                .build();

        when(expensesRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(any(), any()))
                .thenReturn(List.of(e1));

        FinancialReportResponse report = financialReportService.generateReport(start, end);

        assertNotNull(report);
        assertEquals(new BigDecimal("10000.00"), report.totalRevenue());
        assertEquals(new BigDecimal("30000.00"), report.totalExpenses());
        assertEquals(new BigDecimal("-20000.00"), report.netProfitOrLoss());
        assertEquals(FinancialStatus.LOSS, report.status());
    }

    @Test
    void generateReport_withBreakEven_calculatesCorrectly() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        when(visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(any(), any()))
                .thenReturn(List.of());
        when(expensesRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(any(), any()))
                .thenReturn(List.of());

        FinancialReportResponse report = financialReportService.generateReport(start, end);

        assertNotNull(report);
        assertEquals(BigDecimal.ZERO, report.totalRevenue());
        assertEquals(BigDecimal.ZERO, report.totalExpenses());
        assertEquals(BigDecimal.ZERO, report.netProfitOrLoss());
        assertEquals(FinancialStatus.BREAK_EVEN, report.status());
    }

    @Test
    void generateReport_withNullDates_defaultsToMonthToDate() {
        when(visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(any(), any()))
                .thenReturn(List.of());
        when(expensesRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(any(), any()))
                .thenReturn(List.of());

        FinancialReportResponse report = financialReportService.generateReport(null, null);

        assertNotNull(report);
        assertEquals(LocalDate.now().withDayOfMonth(1), report.startDate());
        assertEquals(LocalDate.now(), report.endDate());
    }

    @Test
    void generateReport_withStartDateAfterEndDate_throwsBadRequestException() {
        LocalDate start = LocalDate.of(2026, 8, 31);
        LocalDate end = LocalDate.of(2026, 8, 1);

        assertThrows(BadRequestException.class, () -> financialReportService.generateReport(start, end));
    }

    @Test
    void generateMonthlyReport_calculatesForYearAndMonth() {
        when(visitLogsRepository.findByCheckInTimeBetweenOrderByCheckInTimeAsc(any(), any()))
                .thenReturn(List.of());
        when(expensesRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(any(), any()))
                .thenReturn(List.of());

        FinancialReportResponse report = financialReportService.generateMonthlyReport(2026, 9);
        assertNotNull(report);
        assertEquals(LocalDate.of(2026, 9, 1), report.startDate());
        assertEquals(LocalDate.of(2026, 9, 30), report.endDate());
    }
}
