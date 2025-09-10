package amgn.amu.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class OauthIdentity {
    private Long oauthId;
    private Long userId;
    private String provider;          // KAKAO/GOOGLE/NAVER
    private String providerUserId;    // 소셜 고유 ID
    private String email;
    private String displayName;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

