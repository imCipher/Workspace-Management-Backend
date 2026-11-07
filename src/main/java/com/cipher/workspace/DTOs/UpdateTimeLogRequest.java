package com.cipher.workspace.DTOs;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
public class UpdateTimeLogRequest extends TimeLogUpdateRequest {

    public UpdateTimeLogRequest(UUID visitLogId, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        super(visitLogId, checkInTime, checkOutTime);
    }
}
