package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.common.LoginUser;
import amgn.amu.domain.User;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.oauth_totp.OauthLoginResult;
import amgn.amu.dto.oauth_totp.OauthProfileDto;
import amgn.amu.entity.OauthIdentity;
import amgn.amu.mapper.OauthIdentityMapper;
import amgn.amu.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    private final LoginUser loginUser;

    @Transactional
    public OauthLoginResult upsertAndLogin(HttpServletRequest req, Authentication auth) {
        OauthProfileDto profile = extractProfile(auth);

        // 이미 로그인된 상태에서 소셜 인증을 했다면 연결 플로우
        Long currentUserId = getCurrentSessionUserIdOrNull(req);
        if (currentUserId != null) {
            linkToUser(currentUserId, profile, auth);
            LoginUserDto sessionUser = getLoginUserByUserId(currentUserId);
            setSessionUser(req, sessionUser);
            return new OauthLoginResult(OauthLoginResult.Status.LINKED_AND_LOGGED_IN, sessionUser);
        }

        // 미로그인 상태라면 로그인/회원가입 플로우
        Optional<User> userByLink = oidMapper.findUserByProvider(profile.getProvider(), profile.getProviderUserId());
        if (userByLink.isPresent()) {
            // 연결된 사용자가 존재하면 토큰 갱신 후 로그인
            updateTokens(profile, auth);
            LoginUserDto dto = LoginUserDto.from(userByLink.get());
            setSessionUser(req, dto);
            return new OauthLoginResult(OauthLoginResult.Status.LOGGED_IN, dto);
        }

        // 이메일로 기존 사용자가 있는지 확인
        if (profile.getEmail() != null) {
            Optional<User> userByEmail = userMapper.findByEmail(profile.getEmail());
            if (userByEmail.isPresent()) {
                linkToUser(userByEmail.get().getUserId(), profile, auth);
                LoginUserDto dto = LoginUserDto.from(userByEmail.get());
                setSessionUser(req, dto);
                return new  OauthLoginResult(OauthLoginResult.Status.LOGGED_IN, dto);
            }
        }

        // 신규 소셜 사용자 생성, 연결 후 로그인
        User created = createSocialUser(profile);
        linkToUser(created.getUserId(), profile, auth);
        LoginUserDto dto = LoginUserDto.from(created);
        setSessionUser(req, dto);
        return new OauthLoginResult(OauthLoginResult.Status.SIGNUP_CREATED, dto);
    }

    private Long getCurrentSessionUserIdOrNull(HttpServletRequest req) {
        try {
            return loginUser.userId(req);
        } catch (Exception e) {
            return null;
        }
    }

    private void setSessionUser(HttpServletRequest req, LoginUserDto dto) {
        var session = req.getSession(true);
        session.setAttribute("loginUser", dto);
    }

    private LoginUserDto getLoginUserByUserId(Long userId) {
        User u = userRepository.findByUserId(userId).orElseThrow();
        return LoginUserDto.from(u);
    }

    // 소셜 프로필을 현재 사용자에 연결 후 토큰 저장
    private void linkToUser(Long userId, OauthProfileDto profile, Authentication auth) {
        OauthIdentity link = new OauthIdentity();
        link.setUserId(userId);
        link.setProvider(profile.getProvider());
        link.setProviderUserId(profile.getProviderUserId());
        link.setEmail(profile.getEmail());
        link.setDisplayName(profile.getDisplayName());

        fillTokens(link, auth);

        oidMapper.insertLink(link);
    }

    // 기존 연결 토큰 갱신
    private void updateTokens(OauthProfileDto profile, Authentication auth) {
        OauthIdentity link = new OauthIdentity();
        link.setProvider(profile.getProvider());
        link.setProviderUserId(profile.getProviderUserId());
        fillTokens(link, auth);
        oidMapper.updateTokens(link);
    }

    // 소셜 사용자 전용 기본 정보로 User 생성
    private User createSocialUser(OauthProfileDto p) {
        User u = new User();
        String base = (p.getEmail() != null) ? p.getEmail().split("@")[0] : p.getProviderUserId();

        u.setLoginId(safeLoginId(p.getProvider(), base));
        u.setUserName(p.getDisplayName());
        u.setNickName(p.getDisplayName());
        u.setEmail(p.getEmail());
        u.setPhoneNumber(null);

        u.setPasswordHash(UUID.randomUUID().toString());

        u.setStatus("ACTIVE");
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());

        userMapper.createSocialUser(u);
        return userRepository.findByUserId(u.getUserId()).orElseThrow();
    }

    private String safeLoginId(String provider, String base) {
        String candidate = (provider + "_" + base).toLowerCase().replaceAll("[^a-z0-9_\\-\\.]", "");
        String id = candidate;
        int seq = 1;
        while (userMapper.existsByLoginId(id)) {
            id = candidate + "_" + seq++;
        }
        return id;
    }

    @SuppressWarnings("unchecked")
    private OauthProfileDto extractProfile(Authentication auth) {
        if(auth instanceof OAuth2AuthenticationToken token) {
            String provider = token.getAuthorizedClientRegistrationId().toUpperCase();
            Map<String, Object> attrs = ((Map<String, Object>) token.getPrincipal().getAttributes());

            if("GOOGLE".equals(provider) && token.getPrincipal() instanceof OidcUser oidc) {
                String sub = oidc.getSubject();
                String email = oidc.getEmail();
                String name = (String) oidc.getAttributes().getOrDefault("name", email);
                return new OauthProfileDto(provider, sub, email, name);
            }

            if("KAKAO".equals(provider)) {
                String pid = String.valueOf(attrs.get("id"));
                Map<String, Object> account = (Map<String, Object>) attrs.get("kakao_account");
                String email = account != null ? (String) account.get("email") : null;
                String name = null;
                if(account != null && account.get("profile") instanceof Map profile) {
                    name = (String) profile.getOrDefault("nickname", email);
                }
                return new OauthProfileDto(provider, pid, email, name);
            }

            if("NAVER".equals(provider)) {
                Map<String, Object> resp = (Map<String, Object>) attrs.get("response");
                String pid = (String) resp.get("id");
                String email = (String) resp.get("email");
                String name = (String) resp.getOrDefault("name", email);
                return new OauthProfileDto(provider, pid, email, name);
            }
        }
        throw new IllegalStateException("Unsupported OAuth principal");
    }

    // 토큰, 만료 세팅
    private void fillTokens(OauthIdentity link, Authentication auth) {
        link.setAccessToken(null);
        link.setRefreshToken(null);
        link.setTokenExpiresAt(LocalDateTime.now().plusDays(90));
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
}
