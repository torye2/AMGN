package amgn.amu.dto.oauth_totp;

import lombok.Value;

@Value
public class OauthProfileDto {
    String provider;        // "GOOGLE"/"KAKAO"/"NAVER"
    String providerUserId;  // 소셜 고유 ID
    String email;
    String name;
}

