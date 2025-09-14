package amgn.amu.dto.oauth_totp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OauthUnlinkRequest {
    @NotBlank
    private String provider;
}
