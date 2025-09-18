package amgn.amu.controller;

import amgn.amu.common.ApiResult;
import amgn.amu.common.LoginUser;
import amgn.amu.component.LoginHelper;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.oauth_totp.OauthLinkStatusResponse;
import amgn.amu.dto.oauth_totp.OauthUnlinkRequest;
import amgn.amu.dto.oauth_totp.PendingOauth;
import amgn.amu.service.OauthBridgeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OauthController {

    private final OauthBridgeService bridge;
    private final LoginUser loginUser;

    // 소셜 로그인 성공 후 현재 세션 상태를 확인할 때 호출
    @GetMapping("/me")
    public ApiResult<OauthLinkStatusResponse> me(HttpServletRequest req) {
        Long uid = loginUser.userId(req);
        List<String> linked = bridge.getLinkedProviders(uid);
        boolean canUnlink = linked.size() > 1;
        return ApiResult.ok(new OauthLinkStatusResponse(linked, canUnlink));
    }

    @GetMapping("/link/confirm")
    @Transactional
    public ResponseEntity<?> confirm(HttpServletRequest req,
                                     HttpServletResponse res,
                                     @AuthenticationPrincipal LoginHelper.LoginPrincipal loginUser) {
        HttpSession session = req.getSession(false);
        var po = (session != null) ? (PendingOauth) session.getAttribute("PENDING_OAUTH") : null;
        if (po == null) {
            return ResponseEntity.status(400).body(Map.of(
                    "code", "NO_PENDING",
                    "message", "세션이 만료되었어요. 소셜 로그인을 다시 시도해 주세요."
            ));
        }
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "code","NO_PENDING","message","세션이 만료되었습니다."
            ));
        }
        bridge.linkIdentity(loginUser.userId(), po);
        session.removeAttribute("PENDING_OAUTH");
        return ResponseEntity.ok(Map.of("linked", true, "method", "login"));
    }

    // 연결 해제 (여러 로그인 수단 중 하나를 끊을 때)
    @PostMapping("/unlink")
    public ApiResult<Void> unlink(@RequestBody @Valid OauthUnlinkRequest oreq,
                                  HttpServletRequest req) {
        Long uid = loginUser.userId(req);
        bridge.unlink(uid, oreq.getProvider().toUpperCase());
        return ApiResult.ok(null);
    }

    @GetMapping("/connect/{provider}")
    public void connect(@PathVariable String provider,
                        HttpServletRequest req,
                        HttpServletResponse res) throws IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var session = req.getSession(true);
        Long myUserId = null;

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof LoginHelper.LoginPrincipal lp
                && lp.userId() != null) {
            myUserId = lp.userId();
        }

        // 2) 세션에 넣어둔 loginUser에서도 시도
        if (myUserId == null) {
            var dto = (LoginUserDto) session.getAttribute("loginUser");
            if (dto != null) myUserId = dto.getUserId();
        }

        // 3) 그래도 없으면 미인증 취급
        if (myUserId == null) {
            session.setAttribute("NEXT_URL", "/myPage.html");
            res.sendRedirect("/login.html");
            return;
        }

        session.setAttribute("LINKING_PROVIDER", provider.toLowerCase());
        session.setAttribute("LINK_USER_ID", myUserId);
        session.setAttribute("LINK_RETURN", "/myPage.html#account");

        res.sendRedirect("/oauth2/authorization/" + provider.toLowerCase());
    }

}
