package amgn.amu.component;

import amgn.amu.domain.User;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.mapper.OauthIdentityMapper;
import amgn.amu.mapper.UserMapper;
import amgn.amu.repository.UserRepository;
import amgn.amu.service.util.OAuthProfileResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoginHelper {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OauthIdentityMapper oidMapper;
    private final SecurityContextRepository contextRepo = new HttpSessionSecurityContextRepository();

    public void loginAs(HttpServletRequest req, HttpServletResponse res,
                        Long userId, String loginId, String displayName) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // 2) Principal — 커스텀 객체로 userId를 들고 다니면 이후에 편함
        var principal = new LoginPrincipal(userId, loginId, displayName);

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        req.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepo.saveContext(context, req, res);

        var session = req.getSession(true);
        if (session.getAttribute("loginUser") == null) {
            User u = userRepository.findByUserId(userId).orElse(null);
            var loginUser = LoginUserDto.from(u);
            session.setAttribute("loginUser", loginUser);
        }
    }

    public LoginUserDto fromAuthentication(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;

        Long userId = null;

        if (auth instanceof UsernamePasswordAuthenticationToken) {
            String logindId = auth.getName();
            User u = userMapper.findByLoginId(logindId).orElse(null);
            userId = u.getUserId();
        } else if (auth instanceof OAuth2AuthenticationToken token) {
            String provider = OAuthProfileResolver.providerOf(token);
            Map<String,Object> attrs = token.getPrincipal().getAttributes();
            String pid = OAuthProfileResolver.pidOf(provider, attrs);
            userId = oidMapper.findUserIdByProviderAndPid(provider, pid);
        }

        return (userId != null) ? buildSessionUser(userId) : null;
    }

    public LoginUserDto buildSessionUser(Long userId) {
        var u = userRepository.findByUserId(userId).orElse(null);
        return LoginUserDto.from(u);
    }

    public void putSession(HttpServletRequest req, LoginUserDto loginUserDto) {
        req.getSession(true).setAttribute("loginUser", loginUserDto);
    }

    // 컨트롤러/서비스에서 userId 꺼내 쓸 때 사용하면 편함
    public static record LoginPrincipal(Long userId, String loginId, String name) implements java.io.Serializable {}

}


