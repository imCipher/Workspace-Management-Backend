package com.cipher.workspace.DTOs;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignInRequest {

    @NotBlank(message = "Username or email is required")
    @JsonAlias({"email", "identifier"})
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
