package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.CustomerAnalyticsResponse;
import com.cipher.workspace.DTOs.CustomerVisitAnalyticsDto;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.Services.CustomerAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Customer Analytics", description = "Endpoints for analyzing customer visits, frequency metrics, and regular customer trends (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/api/analytics/customers", "/analytics/customers"})
@PreAuthorize("hasRole('ADMIN')")
public class CustomerAnalyticsController {

    private final CustomerAnalyticsService customerAnalyticsService;

    public CustomerAnalyticsController(CustomerAnalyticsService customerAnalyticsService) {
        this.customerAnalyticsService = customerAnalyticsService;
    }

    @Operation(summary = "Get aggregate customer analytics", description = "Calculates total visits, total customers, regular customer counts, and breakdown lists based on optional filter parameters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer analytics summary returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping
    public ResponseEntity<CustomerAnalyticsResponse> getCustomerAnalytics(
            @Parameter(description = "Filter by calendar year (e.g. 2026)")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "Filter by calendar month (1-12)")
            @RequestParam(required = false) Integer month,

            @Parameter(description = "Visit count threshold to qualify as a regular customer")
            @RequestParam(required = false) Integer threshold,

            @Parameter(description = "Flag to only include regular customers in list")
            @RequestParam(required = false) Boolean onlyRegulars,

            @Parameter(description = "Search term for customer name")
            @RequestParam(required = false) String search,

            @Parameter(description = "Exact name match filter")
            @RequestParam(required = false) String name,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(required = false) Integer page,

            @Parameter(description = "Page size")
            @RequestParam(required = false) Integer size
    ) {
        CustomerAnalyticsResponse response = customerAnalyticsService.getCustomerAnalytics(
                year, month, threshold, onlyRegulars, search, name, page, size
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get paginated regular customers", description = "Retrieves paginated list of regular customers who meet or exceed the monthly visit frequency threshold.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of regular customers returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping("/regulars")
    public ResponseEntity<PagedResponse<CustomerVisitAnalyticsDto>> getRegulars(
            @Parameter(description = "Filter by calendar year")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "Filter by calendar month (1-12)")
            @RequestParam(required = false) Integer month,

            @Parameter(description = "Threshold for regular visitor qualification")
            @RequestParam(required = false) Integer threshold,

            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<CustomerVisitAnalyticsDto> response = customerAnalyticsService.getRegularCustomers(
                year, month, threshold, page, size
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get customer analytics details", description = "Retrieves in-depth visit metrics, total visits, and regular status for an individual customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer analytics details returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CustomerVisitAnalyticsDto> getCustomerDetails(
            @Parameter(description = "Customer UUID")
            @PathVariable UUID id,

            @Parameter(description = "Filter by calendar year")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "Filter by calendar month (1-12)")
            @RequestParam(required = false) Integer month,

            @Parameter(description = "Threshold for regular visitor qualification")
            @RequestParam(required = false) Integer threshold
    ) {
        CustomerVisitAnalyticsDto response = customerAnalyticsService.getCustomerDetails(id, year, month, threshold);
        return ResponseEntity.ok(response);
    }
}
