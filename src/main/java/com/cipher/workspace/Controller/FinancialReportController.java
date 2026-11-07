package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.FinancialReportResponse;
import com.cipher.workspace.Services.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Financial Reports", description = "Endpoints for generating financial statements and profit/loss summaries (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/api/reports", "/reports"})
@PreAuthorize("hasRole('ADMIN')")
public class FinancialReportController {

    private final FinancialReportService financialReportService;

    public FinancialReportController(FinancialReportService financialReportService) {
        this.financialReportService = financialReportService;
    }

    @Operation(summary = "Generate financial report / profit & loss", description = "Calculates total revenue, total expenses, net profit/loss, and breakdown for a given date range.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Financial report generated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinancialReportResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Start date cannot be after end date"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping({"/financial", ""})
    public ResponseEntity<FinancialReportResponse> getFinancialReport(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2026-10-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "End date (YYYY-MM-DD)", example = "2026-10-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        FinancialReportResponse report = financialReportService.generateReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Generate monthly financial report", description = "Calculates monthly revenue, expenses, and net profit for a specific calendar month and year.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Monthly report generated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinancialReportResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid month or year"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping({"/financial/monthly", "/monthly"})
    public ResponseEntity<FinancialReportResponse> getMonthlyReport(
            @Parameter(description = "Filter by calendar year (e.g. 2026)")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "Filter by calendar month (1-12)")
            @RequestParam(required = false) Integer month
    ) {
        FinancialReportResponse report = financialReportService.generateMonthlyReport(year, month);
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Generate month-to-date financial report", description = "Calculates month-to-date revenue, expenses, and net profit from the 1st of the current month up to today.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Month-to-date report generated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinancialReportResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping("/month-to-date")
    public ResponseEntity<FinancialReportResponse> getMonthToDateReport() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();
        FinancialReportResponse report = financialReportService.generateReport(startOfMonth, today);
        return ResponseEntity.ok(report);
    }
}
