package amgn.amu.controller;

import amgn.amu.common.ApiResult;
import amgn.amu.common.LoginUser;
import amgn.amu.dto.PasswordVerifyRequest;
import amgn.amu.dto.UpdateProfileRequest;
import amgn.amu.dto.UserProfileDto;
import amgn.amu.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;
    private final LoginUser loginUser;

    @GetMapping("/profile")
    public ApiResult<UserProfileDto> getProfile(HttpServletRequest req) {
        String loginId = loginUser.loginId(req);
        return ApiResult.ok(profileService.getProfile(loginId));
    }

    @PostMapping("/verify-password")
    public ApiResult<Void> verifyPassword(
            @RequestBody @Valid PasswordVerifyRequest pwreq,
            HttpServletRequest req) {
        String loginId = loginUser.loginId(req);
        boolean ok = profileService.verifyPassword(loginId, pwreq.password());
        return ok ? ApiResult.ok(null) : ApiResult.fail("비밀번호가 일치하지 않습니다.");
    }

    @PutMapping("/profile")
    public ApiResult<Void> updateProfile(
            @RequestBody @Valid UpdateProfileRequest updatereq,
            HttpServletRequest req
            ) {
        String loginId = loginUser.loginId(req);
        profileService.updateProfile(loginId, updatereq);
        return ApiResult.ok(null);
    }
}
