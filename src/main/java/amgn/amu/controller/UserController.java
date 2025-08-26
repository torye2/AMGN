package amgn.amu.controller;

import amgn.amu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 로그인 처리 API
    @PostMapping("/loginProc")
    public String login(@RequestParam("account") String account,
                        @RequestParam("password") String password,
                        Model model,
                        HttpSession session) {

        boolean loginSuccess = userService.login(account, password);

        if (loginSuccess) {
            // 로그인 성공 시 세션에 닉네임 저장
            String nickname = userService.getNicknameByAccount(account);
            session.setAttribute("nickname", nickname);
            return "redirect:/main";
        } else {
            model.addAttribute("error", "아이디 또는 비밀번호가 잘못되었습니다.");
            return "login";
        }
    }

    // 로그인 상태 확인 API
    @GetMapping("/status")
    public Map<String, Object> getUserStatus(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String loggedInUserNickname = (String) session.getAttribute("nickname");

        if (loggedInUserNickname != null) {
            response.put("isLoggedIn", true);
            response.put("nickname", loggedInUserNickname);
        } else {
            response.put("isLoggedIn", false);
        }
        return response;
    }
}