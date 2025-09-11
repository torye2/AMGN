package amgn.amu.controller;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.oauth_totp.BackupCodesResponse;
import amgn.amu.dto.oauth_totp.TotpSetupResponse;
import amgn.amu.dto.oauth_totp.TotpVerifyRequest;
import amgn.amu.service.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/mfa/totp")
@RequiredArgsConstructor
public class MfaController {
    private final TotpService totpService;

    private long currentUserId(HttpServletRequest req) {
        LoginUserDto user = (LoginUserDto) req.getSession().getAttribute("loginUser");
        if (user == null) throw new AppException(ErrorCode.NOT_LOGGED_IN);
        return user.getUserId();
    }

    // QR setup
    @GetMapping("/setup")
    public TotpSetupResponse setup(HttpServletRequest req) {
        long uid = currentUserId(req);
        return totpService.beginSetup(uid, "AMGN", String.valueOf(uid));
    }

    // 코드 검증, 백업코드 발급
    @PostMapping("/activate")
    public BackupCodesResponse activate(@RequestBody TotpVerifyRequest body, HttpServletRequest req) throws Exception {
        long uid = currentUserId(req);
        BackupCodesResponse r = totpService.activate(uid, body.getCode());
        req.getSession().setAttribute("MFA_VERIFIED_AT", Instant.now());
        return r;
    }

    @PostMapping("/verify")
    public String verify(@RequestBody String input, HttpServletRequest req) throws Exception {
        long uid = currentUserId(req);
        String code = input.replaceAll("\\D", "");
        boolean ok = totpService.verifyOrUseBackup(uid, code);
        if (ok) {
            req.getSession().setAttribute("MFA_VERIFIED_AT", Instant.now());
            return "OK";
        }
        return "FAIL";
    }

}
