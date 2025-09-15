package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.domain.User;
import amgn.amu.dto.SignupDto;
import amgn.amu.mapper.UserMapper;
import amgn.amu.service.util.ContactNormalizer;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupService {

    //private final ContactNormalizer normalizer = new ContactNormalizer();
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void regist(SignupDto dto) {
        if (userMapper.existsByLoginId(dto.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (dto.getEmail() != null && userMapper.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (dto.getPhoneNumber() != null && userMapper.existsByPhone(dto.getPhoneNumber())) {
            throw new IllegalArgumentException("이미 사용 중인 휴대전화 번호입니다.");
        }

        String passwordHash = passwordEncoder.encode(dto.getPasswordHash());

        User user = new User();
        user.setLoginId(req(dto.getLoginId(), "아이디"));                    // 필수값은 req(...)로 강제
        user.setPasswordHash(passwordEncoder.encode(req(dto.getPasswordHash(), "비밀번호")));
        user.setUserName(req(dto.getUserName(), "이름"));
        user.setEmail(req(dto.getEmail(), "이메일"));
        user.setEmailNormalized(ContactNormalizer.normalizeEmail(dto.getEmail()));

        // 선택값들은 전부 null-safe 가공
        user.setNickName(defaultIfBlank(dto.getNickName(), dto.getUserName())); // 닉네임 없으면 이름으로
        user.setGender(trimToNull(dto.getGender()));            // "", "   " -> null
        user.setPhoneNumber(trimToNull(dto.getPhoneNumber()));
        String e164 = ContactNormalizer.toE164(dto.getPhoneNumber(), "KR");
        if (dto.getPhoneNumber() != null && e164 == null) {
            throw new IllegalArgumentException("유효하지 않은 휴대폰 번호입니다.");
        }
        user.setPhoneE164(e164);
        user.setPhoneVerified(0);

        user.setBirthYear(dto.getBirthYear());
        user.setBirthMonth(dto.getBirthMonth());
        user.setBirthDay(dto.getBirthDay());

        if (user.getEmailNormalized()!=null && userMapper.existsByEmailNormalized(user.getEmailNormalized()))
            throw new AppException(ErrorCode.DUPLICATE_EMAIL);
        if (user.getPhoneE164()!=null && userMapper.existsByPhoneE164(user.getPhoneE164()))
            throw new AppException(ErrorCode.DUPLICATE_PHONE);

        int row = userMapper.insert(user);
        if (row != 1) {
            throw new IllegalStateException("회원가입 저장 실패");
        }
    }
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private static String defaultIfBlank(String s, String def) {
        if (s == null) return def;
        String t = s.trim();
        return t.isEmpty() ? def : t;
    }
    private static String req(String s, String label) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(label + "은(는) 필수입니다.");
        return s.trim();
    }
}
