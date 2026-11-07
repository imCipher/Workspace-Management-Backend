package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.FinancialReportResponse;
import com.cipher.workspace.Entity.FinancialStatus;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.GlobalExceptionHandler;
import com.cipher.workspace.Services.FinancialReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinancialReportControllerTest {

    @Mock
    private FinancialReportService financialReportService;

    @InjectMocks
    private FinancialReportController financialReportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(financialReportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getFinancialReport_returns200() throws Exception {
        FinancialReportResponse report = new FinancialReportResponse(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                new BigDecimal("50000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("30000.00"),
                FinancialStatus.PROFIT,
                25,
                5,
                Map.of("SHARED_WORKSPACE", new BigDecimal("50000.00")),
                Map.of("UTILITIES", new BigDecimal("20000.00")),
                LocalDateTime.now(),
                UUID.randomUUID(),
                "admin"
        );

        when(financialReportService.generateReport(any(), any())).thenReturn(report);

        mockMvc.perform(get("/api/reports/financial?startDate=2026-10-01&endDate=2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(50000.00))
                .andExpect(jsonPath("$.totalExpenses").value(20000.00))
                .andExpect(jsonPath("$.netProfitOrLoss").value(30000.00))
                .andExpect(jsonPath("$.status").value("PROFIT"));
    }

    @Test
    void getFinancialReport_invalidDates_returns400() throws Exception {
        when(financialReportService.generateReport(any(), any()))
                .thenThrow(new BadRequestException("Start date cannot be after end date"));

        mockMvc.perform(get("/api/reports/financial?startDate=2026-10-31&endDate=2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Start date cannot be after end date"));
    }

    @Test
    void getMonthlyReport_returns200() throws Exception {
        FinancialReportResponse report = new FinancialReportResponse(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                new BigDecimal("10000.00"),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                FinancialStatus.BREAK_EVEN,
                5,
                2,
                Map.of(),
                Map.of(),
                LocalDateTime.now(),
                UUID.randomUUID(),
                "admin"
        );

        when(financialReportService.generateMonthlyReport(2026, 10)).thenReturn(report);

        mockMvc.perform(get("/api/reports/monthly?year=2026&month=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BREAK_EVEN"));
    }

    @Test
    void getMonthToDateReport_returns200() throws Exception {
        FinancialReportResponse report = new FinancialReportResponse(
                LocalDate.now().withDayOfMonth(1),
                LocalDate.now(),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("4000.00"),
                FinancialStatus.PROFIT,
                2,
                1,
                Map.of(),
                Map.of(),
                LocalDateTime.now(),
                null,
                null
        );

        when(financialReportService.generateReport(any(), any())).thenReturn(report);

        mockMvc.perform(get("/api/reports/month-to-date"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROFIT"));
    }
}
