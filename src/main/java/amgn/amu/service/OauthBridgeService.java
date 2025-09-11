package amgn.amu.service;

import amgn.amu.domain.User;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.oauth_totp.OauthProfileDto;
import amgn.amu.entity.OauthIdentity;
import amgn.amu.mapper.OauthIdentityMapper;
import amgn.amu.mapper.UserMapper;
import amgn.amu.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OauthBridgeService {
    private final UserMapper userMapper;
    private final OauthIdentityMapper oidMapper;

    public void upsertAndLogin(HttpServletRequest req, Authentication auth) {
        OauthProfileDto p = extractProfile(auth);

        User user = oidMapper.findUserByProvider(p.getProvider(), p.getProviderUserId())
                .or(() -> Optional.ofNullable(p.getEmail()).flatMap(userMapper::findByEmail))
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(p.getEmail());
                    u.setUserName(Optional.ofNullable(p.getName()).orElse("User"));
                    userMapper.createSocialUser(u);
                    return u;
                });

        OauthIdentity link = new OauthIdentity();
        link.setUserId(user.getUserId());
        link.setProvider(p.getProvider());
        link.setProviderUserId(p.getProviderUserId());
        link.setEmail(p.getEmail());
        link.setDisplayName(p.getName());
        oidMapper.insertLink(link);

        LoginUserDto dto = LoginUserDto.from(user);
        req.getSession(true).setAttribute("loginUser", dto);
    }

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
}
