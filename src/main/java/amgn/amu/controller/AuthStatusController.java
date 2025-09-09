package amgn.amu.controller;

import amgn.amu.dto.LoginUserDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthStatusController {

    @GetMapping("/api/user/status")
    public Map<String, Object> status(HttpSession session, HttpServletResponse res) {
        // 캐시 금지 (로그인/로그아웃 직후 UI 즉시 반영용)
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        res.setHeader("Pragma", "no-cache");

        LoginUserDto u = (LoginUserDto) session.getAttribute("loginUser");
        return Map.of(
                "isLoggedIn", u != null,
                "username", u != null ? u.getUserName() : null,
                "nickname",   u != null ? u.getNickName() : null,
                "userId",     u != null ? u.getLoginId()      : null,
                "createdAt", u != null ? u.getCreatedAt().toLocalDate().toString() : null
        );
    }
}

