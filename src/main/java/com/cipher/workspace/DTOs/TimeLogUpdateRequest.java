package com.cipher.workspace.DTOs;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeLogUpdateRequest {

    @NotNull(message = "Visit log ID is required")
    private UUID visitLogId;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;
}
