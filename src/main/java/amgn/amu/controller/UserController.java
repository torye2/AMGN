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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    record MeResponse(boolean loggedIn, Long userId, String nickname, List<String> roles, boolean isAdmin) {}
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/me")
    public MeResponse getLoginUser(Authentication authentication, HttpSession session) {
        boolean loggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        var authorities = loggedIn ? authentication.getAuthorities() : List.<GrantedAuthority>of();
        List<String> roles = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        boolean isAdmin = roles.stream().anyMatch(r -> r.contains("ADMIN"));

        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        String nickname = loginUser != null ? loginUser.getNickName() : null;

        if (loginUser == null && loggedIn) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                String username = userDetails.getUsername();
            } else if (principal instanceof OAuth2User ou) {
                // ou.getAttributes()
            }
        }

        return new MeResponse(loggedIn, userId, nickname, roles, isAdmin);
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
