package amgn.amu.controller;

import amgn.amu.common.ApiResult;
import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.domain.User;
import amgn.amu.dto.DeleteAccountRequest;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.repository.UserRepository;
import amgn.amu.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

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

    @DeleteMapping("/delete")
    public ApiResult<Void> deleteMe(@RequestBody @Valid DeleteAccountRequest req, HttpSession session) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) return ApiResult.fail("로그인 된 유저를 찾을 수 없습니다.");

        java.time.Instant until = (session == null) ? null : (java.time.Instant) session.getAttribute("reauth_ok_until");
        if (until == null || java.time.Instant.now().isAfter(until)) {
            throw new AppException(ErrorCode.REAUTH_REQUIRED);
        }

        userService.deleteMe(loginUser.getUserId(), req, true);

        session.invalidate();
        return ApiResult.ok(null);
    }
}
