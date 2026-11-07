package com.cipher.workspace.DTOs;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @JsonAlias({"oldPassword", "current_password", "old_password"})
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    @JsonAlias({"new_password", "password"})
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @JsonAlias({"confirmPassword", "passwordConfirm", "newPasswordConfirm", "confirm_password", "new_password_confirm"})
    private String confirmPassword;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "New passwords do not match")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
