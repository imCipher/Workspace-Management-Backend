package com.cipher.workspace.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Schema(description = "Request body for updating an existing expense category")
public class UpdateExpenseCategoryRequest extends UpdateOptionRequest {

    public UpdateExpenseCategoryRequest(String name, String displayName) {
        super(name, displayName);
    }
}
