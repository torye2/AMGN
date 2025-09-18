package amgn.amu.controller;

import amgn.amu.common.LoginUser;
import amgn.amu.component.LoginHelper;
import amgn.amu.domain.User;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.OnboardingRequest;
import amgn.amu.dto.oauth_totp.PendingOauth;
import amgn.amu.mapper.UserMapper;
import amgn.amu.repository.UserRepository;
import amgn.amu.service.OauthBridgeService;
import amgn.amu.service.util.ContactNormalizer;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class OnboardingController {

    private final UserMapper userMapper;
    private final OauthBridgeService bridge;
    private final LoginHelper loginHelper;
    private final UserRepository userRepository;

    @GetMapping("/onboarding")
    public ModelAndView onboardingPage(HttpServletRequest req) {
        var s = req.getSession(false);
        var po = (s != null) ? (PendingOauth) s.getAttribute("PENDING_OAUTH") : null;
        if(po == null) return new ModelAndView("redirect:/login");
        return new ModelAndView("redirect:/onboarding.html");
    }

    @PostMapping("/api/onboarding")
    @Transactional
    public ResponseEntity<?> complete(@RequestBody @Valid OnboardingRequest body,
                                      HttpServletRequest req,
                                      HttpServletResponse res) {
        HttpSession session = req.getSession(false);
        PendingOauth po = (session != null) ? (PendingOauth) session.getAttribute("PENDING_OAUTH") : null;
        if (po == null) return ResponseEntity.status(400).body(Map.of(
                "code", "NO_PENDING",
                "message", "세션이 만료되었어요. 소셜 로그인을 다시 시도해 주세요."
        ));

        if (po.getEmail() != null && po.isEmailVerified()) {
            User byEmail = userMapper.findByEmailNormalized(ContactNormalizer.normalizeEmail(po.getEmail()))
                    .orElse(null);
            if (byEmail != null) {
                return ResponseEntity.status(202).body(Map.of(
                        "code","LOGIN_TO_LINK",
                        "message","기존 계정으로 로그인하면 연결됩니다.",
                        "nextUrl","/login?next=/api/oauth/link/confirm"
                ));
            }
        }

        String raw = body.getPhoneNumber();
        String e164 = toE164(raw, "KR");
        if (e164 == null) {
            return ResponseEntity.status(400).body(Map.of(
                    "code", "INVALID_PHONE",
                    "message", "휴대폰 번호 형식이 올바르지 않습니다."
            ));
        } else {
            User owner = userMapper.findByPhoneE164(e164).orElse(null);
            Long ownerId = owner.getUserId();
            if (ownerId != null) {
                return ResponseEntity.status(202).body(Map.of(
                        "code","LOGIN_TO_LINK",
                        "message","기존 계정으로 로그인하면 연결됩니다.",
                        "nextUrl","/login?next=/api/oauth/link/confirm"
                ));
            }
        }

        long userId = bridge.createUserFromPending(po, raw, e164);
        bridge.linkIdentity(userId, po);
        session.removeAttribute("PENDING_OAUTH");
        loginHelper.loginAs(req, res, userId, null, null);
        LoginUserDto dto = (LoginUserDto) req.getSession(true).getAttribute("loginUser");

        if (dto == null) {
            dto = LoginUserDto.from(userRepository.findByUserId(userId).get());
            req.getSession(true).setAttribute("loginUser", dto);
        }

        return ResponseEntity.ok(Map.of("created", true));
    }

    private String toE164(String raw, String defaultRegion) {
        if (raw == null || raw.isBlank()) return null;
        var util = PhoneNumberUtil.getInstance();
        try {
            var num = util.parse(raw, defaultRegion);
            if (!util.isValidNumber(num)) return null;
            return util.format(num, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            return null;
        }
    }
}
