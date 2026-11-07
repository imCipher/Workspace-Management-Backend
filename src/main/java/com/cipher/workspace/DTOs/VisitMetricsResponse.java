package com.cipher.workspace.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Summary of visit metrics and status breakdown for the workspace")
public record VisitMetricsResponse(
        @Schema(description = "Date for the report", example = "2026-11-06")
        LocalDate date,

        @Schema(description = "Start date of the filtered period", example = "2026-11-01")
        LocalDate startDate,

        @Schema(description = "End date of the filtered period", example = "2026-11-30")
        LocalDate endDate,

        @Schema(description = "Calendar year of the period", example = "2026")
        Integer year,

        @Schema(description = "Calendar month of the period (1-12)", example = "11")
        Integer month,

        @Schema(description = "Total number of visits recorded in the period", example = "24")
        long totalVisits,

        @Schema(description = "Number of active visits currently in-space", example = "6")
        long activeVisits,

        @Schema(description = "Number of completed visits checked out in this period", example = "18")
        long completedVisits,

        @Schema(description = "Number of returning customers for that period", example = "14")
        long returningCustomers,

        @Schema(description = "Number of new first-time customers for that period", example = "4")
        long newCustomers
) {
    public VisitMetricsResponse(LocalDate date, long totalVisits, long activeVisits, long completedVisits) {
        this(date, date, date, date != null ? date.getYear() : null, date != null ? date.getMonthValue() : null,
                totalVisits, activeVisits, completedVisits, 0L, 0L);
    }
}
