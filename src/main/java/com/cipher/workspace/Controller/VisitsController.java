package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Services.VisitsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Visits & Check-in Desk", description = "Endpoints for handling desk check-in, check-out, time log editing, visit history, and real-time space metrics")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/api/visits", "/visits"})
@PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
public class VisitsController {

    private final VisitsService visitsService;

    public VisitsController(VisitsService visitsService) {
        this.visitsService = visitsService;
    }

    @Operation(summary = "Check in visitor", description = "Records visitor check-in, assigns workspace desk/area, and auto-provisions or increments customer profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Check-in recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid check-in request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/check-in")
    public ResponseEntity<VisitResponse> checkIn(
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        VisitResponse response = visitsService.checkIn(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Check out visitor", description = "Records departure timestamp for an active visit log, calculating total duration in minutes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visitor checked out successfully"),
            @ApiResponse(responseCode = "400", description = "Visit already checked out"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Visit log not found")
    })
    @PostMapping("/check-out")
    public ResponseEntity<VisitResponse> checkOut(
            @Valid @RequestBody CheckOutRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        VisitResponse response = visitsService.checkOut(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supervised time log correction (PUT)", description = "Updates check-in and/or check-out timestamps with staff audit trail.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Time log updated successfully"),
            @ApiResponse(responseCode = "400", description = "Check-out time cannot be before check-in time"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Visit log not found")
    })
    @PutMapping("/time-log")
    public ResponseEntity<VisitResponse> updateTimeLog(
            @Valid @RequestBody UpdateTimeLogRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        VisitResponse response = visitsService.updateTimeLog(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supervised time log correction (PATCH)", description = "Partially updates check-in or check-out timestamp.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Time log updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid timestamps"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Visit log not found")
    })
    @PatchMapping("/time-log")
    public ResponseEntity<VisitResponse> patchTimeLog(
            @Valid @RequestBody UpdateTimeLogRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        VisitResponse response = visitsService.updateTimeLog(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all visit logs", description = "Retrieves filterable and paginated visit logs along with calculated summary metrics.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated visit history returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<VisitLogsPagedResponse> getAllVisits(
            @Parameter(description = "Start date for range filter (YYYY-MM-DD)", example = "2026-10-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "End date for range filter (YYYY-MM-DD)", example = "2026-10-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @Parameter(description = "Specific calendar date (YYYY-MM-DD)", example = "2026-10-15")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @Parameter(description = "Filter by calendar year (e.g. 2026)")
            @RequestParam(required = false)
            Integer year,

            @Parameter(description = "Filter by calendar month (1-12)")
            @RequestParam(required = false)
            Integer month,

            @Parameter(description = "Filter by visit status: ACTIVE or COMPLETED")
            @RequestParam(required = false)
            String status,

            @Parameter(description = "Filter by customer UUID")
            @RequestParam(required = false)
            UUID customerId,

            @Parameter(description = "Search across customer name, phone number, or space assigned")
            @RequestParam(required = false)
            String search,

            @Parameter(description = "Filter by service type")
            @RequestParam(required = false)
            String serviceType,

            @Parameter(description = "Filter by payment type")
            @RequestParam(required = false)
            String paymentType,

            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10")
            int size
    ) {
        VisitLogsPagedResponse response = visitsService.getAllVisits(
                startDate, endDate, date, year, month, status, customerId, search, serviceType, paymentType, page, size
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get currently active in-space visits", description = "Retrieves paginated list of visitors currently checked in.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated active visits returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/active")
    public ResponseEntity<PagedResponse<VisitResponse>> getActiveVisits(
            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<VisitResponse> response = visitsService.getActiveVisits(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get visit metrics and summary", description = "Retrieves total visits, active visits, completed visits, and returning/new customer statistics.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Visit metrics returned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VisitMetricsResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/metrics")
    public ResponseEntity<VisitMetricsResponse> getVisitMetrics(
            @Parameter(description = "Filter by specific date (YYYY-MM-DD)", example = "2026-10-15")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @Parameter(description = "Start date for range filter (YYYY-MM-DD)", example = "2026-10-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "End date for range filter (YYYY-MM-DD)", example = "2026-10-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @Parameter(description = "Filter by calendar year (e.g. 2026)")
            @RequestParam(required = false)
            Integer year,

            @Parameter(description = "Filter by calendar month (1-12)")
            @RequestParam(required = false)
            Integer month
    ) {
        VisitMetricsResponse response = visitsService.getVisitMetrics(date, startDate, endDate, year, month);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get today's desk metrics", description = "Returns active visitors, today's check-ins, and today's total revenue.")
    @GetMapping("/today-metrics")
    public ResponseEntity<Map<String, Object>> getTodayMetrics() {
        return ResponseEntity.ok(visitsService.getTodayMetrics());
    }

    @Operation(summary = "Get payment types", description = "Retrieves available payment types for desk check-in.")
    @GetMapping("/payment-types")
    public ResponseEntity<List<EnumOptionResponse>> getPaymentTypes() {
        return ResponseEntity.ok(visitsService.getPaymentTypes());
    }

    @Operation(summary = "Get service types", description = "Retrieves available workspace service types.")
    @GetMapping("/service-types")
    public ResponseEntity<List<EnumOptionResponse>> getServiceTypes() {
        return ResponseEntity.ok(visitsService.getServiceTypes());
    }

    @Operation(summary = "Get visit statuses", description = "Retrieves available visit status options (ACTIVE, COMPLETED).")
    @GetMapping("/statuses")
    public ResponseEntity<List<EnumOptionResponse>> getVisitStatuses() {
        return ResponseEntity.ok(visitsService.getVisitStatuses());
    }

    @Operation(summary = "Get visit metadata", description = "Retrieves all visit-related dropdown options (service types, payment types, visit statuses).")
    @GetMapping("/metadata")
    public ResponseEntity<VisitMetadataResponse> getVisitMetadata() {
        return ResponseEntity.ok(visitsService.getVisitMetadata());
    }

    @Operation(summary = "Get visit options", description = "Convenience alias for visit metadata dropdown options.")
    @GetMapping("/options")
    public ResponseEntity<VisitMetadataResponse> getVisitOptions() {
        return ResponseEntity.ok(visitsService.getVisitMetadata());
    }
}
