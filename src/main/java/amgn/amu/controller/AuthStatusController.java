package amgn.amu.controller;

import amgn.amu.dto.LoginUserDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class AuthStatusController {

    @GetMapping("/api/user/status")
    public Map<String, Object> status(HttpSession session, HttpServletResponse res) {
        // 캐시 금지 (로그인/로그아웃 직후 UI 즉시 반영용)
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        res.setHeader("Pragma", "no-cache");

        String sid = session.getId();
        LoginUserDto u = (LoginUserDto) session.getAttribute("loginUser");
        System.out.println("[STATUS] sid=" + sid + " loginUser=" + (u==null? "null" : u.getLoginId()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isLoggedIn", u != null);
        result.put("loggedIn", u != null);

        if (u != null) {
            result.put("username",  u.getUserName());
            result.put("nickname",  u.getNickName());
            result.put("userId",    u.getUserId());
            result.put("loginId",   u.getLoginId());
            result.put("createdAt", u.getCreatedAt() != null
                    ? u.getCreatedAt().toLocalDate().toString()
                    : null);
        }
        return result;
    }
}

