package amgn.amu.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LoginHelper {

    private final SecurityContextRepository contextRepo = new HttpSessionSecurityContextRepository();

    public void loginAs(HttpServletRequest req, HttpServletResponse res,
                        Long userId, String loginId, String displayName) {
        // 1) 권한 구성(없으면 기본 ROLE_USER)
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // 2) Principal — 커스텀 객체로 userId를 들고 다니면 이후에 편함
        var principal = new LoginPrincipal(userId, loginId, displayName);

        // 3) 패스워드는 필요 없음(소셜/프로그램틱 로그인)
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        // (선택) 세션 고정 공격 방지 — Spring Security 필터가 자동으로 하진 않으므로 수동 호출 권장
        req.changeSessionId();

        // 4) SecurityContext에 저장 + 세션에 영속화
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepo.saveContext(context, req, res);
    }

    // 컨트롤러/서비스에서 userId 꺼내 쓸 때 사용하면 편함
    public static record LoginPrincipal(Long userId, String loginId, String name) implements java.io.Serializable {}

}


