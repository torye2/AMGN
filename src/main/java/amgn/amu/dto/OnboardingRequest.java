package amgn.amu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OnboardingRequest {
    @NotBlank
    private String phoneNumber;
}
