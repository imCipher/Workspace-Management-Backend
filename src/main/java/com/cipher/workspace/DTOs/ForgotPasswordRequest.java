package com.cipher.workspace.DTOs;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @JsonAlias({"username", "identifier"})
    private String email;
}
