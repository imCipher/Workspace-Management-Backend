package com.cipher.workspace.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Schema(description = "Request body for creating a new expense category")
public class CreateExpenseCategoryRequest extends CreateOptionRequest {

    public CreateExpenseCategoryRequest(String name, String displayName) {
        super(name, displayName);
    }
}
