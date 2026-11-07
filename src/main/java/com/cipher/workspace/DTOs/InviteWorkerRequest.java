package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteWorkerRequest {

    @NotBlank(message = "Full name is required")
    private String fullname;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @Builder.Default
    private Role role = Role.WORKER;
}
