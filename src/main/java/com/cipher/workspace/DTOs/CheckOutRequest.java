package com.cipher.workspace.DTOs;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckOutRequest {

    @NotNull(message = "Visit log ID is required")
    private UUID visitLogId;
}
