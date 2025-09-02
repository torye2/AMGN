package amgn.amu.controller;

import amgn.amu.dto.LoginUserDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public Map<String, Object> getLoginUser(HttpSession session) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("loggedIn", false);
        }
        return Map.of(
            "loggedIn", true,
            "userId", loginUser.getUserId(),
            "nickname", loginUser.getNickName()
        );
    }
}
