package amgn.amu.dto.oauth_totp;

import lombok.Data;

@Data
public class TotpVerifyRequest {
    private int code;
}
