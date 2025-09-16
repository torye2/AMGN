package amgn.amu.controller;

import amgn.amu.domain.User;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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

    @GetMapping("/nickname/{userId}")
    public Map<String, Object> getNickname(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Map.of("nickname", "알 수 없음");
        return Map.of("nickname", user.getNickName());
    }
}
