package amgn.amu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String passwordHash;
    @NotBlank
    private String status;
}
