package amgn.amu.dto.oauth_totp;

import lombok.Value;

@Value
public class TotpSetupResponse {
    String qrDataUrl;
    String maskedSecret;
}

