package com.cipher.workspace.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Paginated list of visit logs with aggregate metrics for the period (total visits, active visits, completed visits, returning customers)")
public record VisitLogsPagedResponse(
        @Schema(description = "List of visit records in the current page")
        List<VisitResponse> content,

        @Schema(description = "Zero-based page number", example = "0")
        int page,

        @Schema(description = "Page size", example = "10")
        int size,

        @Schema(description = "Total number of matching records across all pages", example = "42")
        long totalElements,

        @Schema(description = "Total number of pages", example = "5")
        int totalPages,

        @Schema(description = "Whether this is the first page", example = "true")
        boolean first,

        @Schema(description = "Whether this is the last page", example = "false")
        boolean last,

        @Schema(description = "Whether the page is empty", example = "false")
        boolean empty,

        @Schema(description = "Total visits recorded in this scope / period", example = "42")
        long totalVisits,

        @Schema(description = "Active visits currently checked in", example = "8")
        long activeVisits,

        @Schema(description = "Completed visits checked out in this scope", example = "34")
        long completedVisits,

        @Schema(description = "Returning customers count for the month/period", example = "15")
        long returningCustomers
) {
    public static VisitLogsPagedResponse of(
            List<VisitResponse> content,
            int page,
            int size,
            long totalElements,
            long totalVisits,
            long activeVisits,
            long completedVisits,
            long returningCustomers
    ) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean first = page <= 0;
        boolean last = totalPages == 0 || page >= totalPages - 1;
        boolean empty = content == null || content.isEmpty();
        return new VisitLogsPagedResponse(
                content != null ? content : List.of(),
                page,
                size,
                totalElements,
                totalPages,
                first,
                last,
                empty,
                totalVisits,
                activeVisits,
                completedVisits,
                returningCustomers
        );
    }

    public static VisitLogsPagedResponse from(
            Page<VisitResponse> page,
            long totalVisits,
            long activeVisits,
            long completedVisits,
            long returningCustomers
    ) {
        return new VisitLogsPagedResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty(),
                totalVisits,
                activeVisits,
                completedVisits,
                returningCustomers
        );
    }
}
