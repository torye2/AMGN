package amgn.amu.controller;

import amgn.amu.common.ApiResult;
import amgn.amu.common.LoginUser;
import amgn.amu.dto.oauth_totp.OauthLinkStatusResponse;
import amgn.amu.dto.oauth_totp.OauthUnlinkRequest;
import amgn.amu.service.OauthBridgeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 연결 해제 (여러 로그인 수단 중 하나를 끊을 때)
    @PostMapping("/unlink")
    public ApiResult<Void> unlink(@RequestBody @Valid OauthUnlinkRequest oreq,
                                  HttpServletRequest req) {
        Long uid = loginUser.userId(req);
        bridge.unlink(uid, oreq.getProvider().toUpperCase());
        return ApiResult.ok(null);
    }

}
