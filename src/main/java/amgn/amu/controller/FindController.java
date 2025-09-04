package amgn.amu.controller;

import amgn.amu.common.ApiResult;
import amgn.amu.dto.*;
import amgn.amu.service.FindService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FindController {
    private final FindService findService;

    @PostMapping("/find-id")
    public ApiResult<FindIdResponse> findId(@Valid @RequestBody FindIdRequest req) {
        return ApiResult.ok(findService.findId(req));
    }

    @PostMapping("/pw-reset/check")
    public ApiResult<ResetTokenResponse> verifyAndToken(@Valid @RequestBody FindPwRequest req) {
        return ApiResult.ok(findService.verifyAndToken(req));
    }

    @PostMapping("/pw-reset/commit")
    public ApiResult<Void> resetPassword(@Valid @RequestBody ResetPwRequest req) {
        findService.resetPassword(req);
        return ApiResult.ok(null);
    }
}
