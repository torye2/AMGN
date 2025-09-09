package amgn.amu.common;

import amgn.amu.dto.LoginUserDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class LoginUser {
    public String loginId(HttpServletRequest req) {
        var session = req.getSession(false);
        if (session != null) {
            Object loginUser = session.getAttribute("loginUser");
            if (loginUser instanceof LoginUserDto dto) {
                return dto.getLoginId();
            }
        }
        throw new IllegalStateException("로그인 된 사용자를 식별할 수 없습니다.");
    }
}
