package amgn.amu.controller;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.common.LoginUser;
import amgn.amu.component.MfaInterceptor;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.oauth_totp.BackupCodesResponse;
import amgn.amu.dto.oauth_totp.TotpSetupResponse;
import amgn.amu.dto.oauth_totp.TotpVerifyRequest;
import amgn.amu.service.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/mfa/totp")
@RequiredArgsConstructor
public class MfaController {
    private final TotpService totpService;
    private final LoginUser loginUser;

    private long currentUserId(HttpServletRequest req) {
        LoginUserDto user = (LoginUserDto) req.getSession().getAttribute("loginUser");
        if (user == null) throw new AppException(ErrorCode.NOT_LOGGED_IN);
        return user.getUserId();
    }

    // 유틸: 요청 본문에서 code만 꺼내고 문자열로 표준화
    private String parseCode(String body) {
        if (body == null) throw new AppException(ErrorCode.BAD_REQUEST, "empty body");
        String s = body.trim();
        try {
            // JSON 오브젝트 형태 {"code": ...}
            if (s.startsWith("{")) {
                com.fasterxml.jackson.databind.JsonNode n =
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(s);
                if (!n.hasNonNull("code")) {
                    throw new AppException(ErrorCode.BAD_REQUEST, "missing 'code'");
                }
                s = n.get("code").asText(); // 숫자여도 문자열로 뽑힘(앞자리 0 보존)
            }
            // JSON 문자열 형태 "123456" → 실제 문자열로 디코드
            else if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
                s = new com.fasterxml.jackson.databind.ObjectMapper().readValue(s, String.class);
            }
            // 그 외: 숫자/문자열이 그대로 온 경우
            // s 그대로 사용
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "invalid json", e);
        }

        // 공백 제거
        s = s.replaceAll("\\s+", "");

        // 형식 검사(TOTP: 6자리 숫자)
        if (!s.matches("\\d{6}")) {
            throw new AppException(ErrorCode.BAD_REQUEST, "invalid totp format");
        }
        return s;
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
    public ResponseEntity<String> verify(@RequestBody String codeBody, HttpServletRequest req) throws Exception {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER") == null) {
            return ResponseEntity.status(401).body("UNAUTHORIZED");
        }
        String code = parseCode(codeBody);
        long userId = ((LoginUserDto) session.getAttribute("LOGIN_USER")).getUserId();

        boolean ok = totpService.verifyOrUseBackup(userId, code); // ±1 step 허용
        if (!ok) return ResponseEntity.status(400).body("INVALID");

        session.setAttribute(MfaInterceptor.SESSION_KEY, java.time.Instant.now());

        // ★ 기대 이유 → 검증 이유로 복사
        Object exp = session.getAttribute(MfaInterceptor.SESSION_EXPECTED_REASON);
        if (exp != null) {
            session.setAttribute(MfaInterceptor.SESSION_VERIFIED_REASON, exp.toString());
            // (선택) 한 번 썼으면 기대값은 지워도 OK
            // session.removeAttribute(MfaInterceptor.SESSION_EXPECTED_REASON);
        }

        System.out.println("[MFA] verify OK, sessionId=" + session.getId() +
                ", verifiedReason=" + session.getAttribute(MfaInterceptor.SESSION_VERIFIED_REASON));

        return ResponseEntity.ok("OK");
    }

    @PostMapping("/disable")
    public ResponseEntity<String> disable(HttpServletRequest req, HttpSession session) {
        long uid = currentUserId(req);
        Instant at = (Instant) session.getAttribute("REAUTH_AT");
        if (at == null || Duration.between(at, Instant.now()).toMinutes() > 5) {
            return ResponseEntity.status(401)
                    .header("X-Reauth-Required", "1")
                    .body("Reauth required");
        }
        totpService.disabled(uid);
        req.getSession().removeAttribute("MFA_VERIFIED_AT");
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/status")
    public Map<String, Object> status(HttpServletRequest req) {
        long uid = currentUserId(req);
        boolean enabled = totpService.isEnabled(uid);
        return Map.of("enabled", enabled);
    }
}
