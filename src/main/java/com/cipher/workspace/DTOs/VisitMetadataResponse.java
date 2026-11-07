package com.cipher.workspace.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Grouped metadata containing available options for visit dropdowns and filters")
public record VisitMetadataResponse(
        @Schema(description = "Available workspace service types")
        List<EnumOptionResponse> serviceTypes,

        @Schema(description = "Available payment types")
        List<EnumOptionResponse> paymentTypes,

        @Schema(description = "Available visit statuses")
        List<EnumOptionResponse> statuses
) {
}
