package amgn.amu.dto.oauth_totp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingOauth {
    private String provider;
    private String providerUserId;
    private String email;
    private boolean emailVerified;
    private String displayName;
    private String accessToken;
    private String refreshToken;
    private Instant tokenExpiresAt;
}
