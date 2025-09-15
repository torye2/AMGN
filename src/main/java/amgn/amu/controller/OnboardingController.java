package amgn.amu.controller;

import amgn.amu.common.LoginUser;
import amgn.amu.domain.User;
import amgn.amu.dto.OnboardingRequest;
import amgn.amu.mapper.UserMapper;
import amgn.amu.repository.UserRepository;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OnboardingController {
    private final UserRepository userRepository;
    private final LoginUser loginUser;
    private final UserMapper userMapper;

    @GetMapping("/onboarding")
    public ModelAndView onboardingPage(HttpServletRequest req) {
        Long uid = loginUser.userId(req);
        User u = userRepository.findByUserId(uid).orElseThrow();
        if(u.getProfileCompleted() != null && u.getProfileCompleted() == 1){
            return new ModelAndView("redirect:/main");
        }
        return new ModelAndView("redirect:/onboarding.html");
    }

    @PostMapping("/api/onboarding")
    public ResponseEntity<?> complete(@RequestBody @Valid OnboardingRequest body,
                                      HttpServletRequest req) {
        Long uid = loginUser.userId(req);
        String raw = body.getPhoneNumber();
        String e164 = toE164(raw, "KR");
        if (e164 == null) {
            return ResponseEntity.badRequest().body(Map.of("message","유효하지 않은 휴대폰 번호입니다."));
        }
        User owner = userMapper.findByPhoneE164(e164).orElseThrow();
        if (owner == null || owner.getUserId().equals(uid)) {
            userMapper.completeOnboarding(uid, raw, e164);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.ok().build();
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
