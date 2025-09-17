package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.common.LoginUser;
import amgn.amu.domain.User;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.oauth_totp.OauthLoginResult;
import amgn.amu.dto.oauth_totp.OauthProfileDto;
import amgn.amu.dto.oauth_totp.PendingOauth;
import amgn.amu.entity.OauthIdentity;
import amgn.amu.mapper.OauthIdentityMapper;
import amgn.amu.mapper.UserMapper;
import amgn.amu.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OauthBridgeService {
    private final UserMapper userMapper;
    private final OauthIdentityMapper oidMapper;
    private final UserRepository userRepository;

    public Optional<Long> findLinkedUserId(String provider, String pid) {
        return Optional.ofNullable(oidMapper.findUserIdByProviderAndPid(provider, pid));
    }

    public long createUserFromPending(PendingOauth po, String phoneRaw, String phoneE164) {
        User u = new User();
        u.setLoginId("SOC_" + UUID.randomUUID());
        u.setEmail(po.getEmail());
        u.setEmailNormalized(normalizeEmail(po.getEmail()));
        u.setUserName(po.getDisplayName());
        u.setPhoneNumber(phoneRaw);
        u.setPhoneE164(phoneE164);
        u.setPhoneVerified(0);
        u.setProfileCompleted(0);
        u.setStatus("ACTIVE");
        userMapper.insert(u); // PK 세팅
        return u.getUserId();
    }

    public void linkIdentity(long userId, PendingOauth po) {
        OauthIdentity identity = new OauthIdentity();
        identity.setUserId(userId);
        identity.setEmail(po.getEmail());
        identity.setProvider(po.getProvider());
        identity.setDisplayName(po.getDisplayName());
        identity.setProviderUserId(po.getProviderUserId());
        identity.setAccessToken(po.getAccessToken());
        identity.setRefreshToken(po.getRefreshToken());
        identity.setTokenExpiresAt(po.getTokenExpiresAt());

        oidMapper.insertLink(identity);
    }

    // 토큰, 만료 세팅
    private void fillTokens(OauthIdentity link, Authentication auth) {
        link.setAccessToken(null);
        link.setRefreshToken(null);
    }

    @Transactional(readOnly = true)
    public List<String> getLinkedProviders(Long userId) {
        List<String> providers = oidMapper.findProvidersByUserId(userId);
        return (providers == null) ? List.of() : providers;
    }

    @Transactional
    public void unlink(Long userId, String provider) {
        String p = provider.toUpperCase(Locale.ROOT);
        List<String> linked = oidMapper.findProvidersByUserId(userId);
        if (linked == null || linked.size() <= 1) {
            throw new IllegalStateException("연결된 로그인 수단이 1개뿐이라 해제할 수 없습니다.");
        }
        int delete = oidMapper.deleteLinkByUserAndProvider(userId, provider);
        if (delete == 0) {
            throw new AppException(ErrorCode.OAUTH_NOT_FOUND, p);
        }
    }

    @Transactional(readOnly = true)
    public boolean isOnboardingRequired(Long userId) {
        User u = userRepository.findByUserId(userId).orElseThrow();
        if (u.getProfileCompleted() != null && u.getProfileCompleted() == 1) return false;
        return (u.getPhoneNumber() == null || u.getPhoneNumber().isBlank());
    }

    private String normalizeEmail(String raw) {
        if (raw == null) return null;
        return java.text.Normalizer.normalize(raw.trim(), java.text.Normalizer.Form.NFKC)
                .toLowerCase(java.util.Locale.ROOT);
    }

}
