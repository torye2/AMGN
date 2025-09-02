package amgn.amu.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordVerifyRequest(
        @NotBlank String password
) {}
