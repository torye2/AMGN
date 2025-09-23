package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.domain.User;
import amgn.amu.dto.DeleteAccountRequest;
import amgn.amu.mapper.OauthIdentityMapper;
import amgn.amu.mapper.UserMapper;
import amgn.amu.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final OauthIdentityMapper oidMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public boolean login(String account, String password) {
        // 데이터베이스에서 아이디로 사용자를 찾음
        Optional<User> userOptional = userRepository.findByLoginId(account);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 사용자가 존재하면 입력된 비밀번호와 데이터베이스의 비밀번호를 비교함
            if (user.getPasswordHash().equals(password)) {
                return true; // 비밀번호 일치, 로그인 성공
            }
        }
        return false; // 사용자 없음 또는 비밀번호 불일치, 로그인 실패
    }

    @Transactional
    public void deleteMe(Long userId, DeleteAccountRequest req, boolean reauthOk) {
        User u = userRepository.findById(userId).orElse(null);
        if (u == null) throw new AppException(ErrorCode.NOT_FOUND_USER);
        if ("DELETED".equalsIgnoreCase(u.getStatus())) return;

        boolean hasPw = u.getPasswordHash() != null && !u.getPasswordHash().isBlank();
        if (hasPw && !reauthOk) {
            String raw = req.getPassword();
            if (raw == null || raw.isBlank() || !passwordEncoder.matches(raw, u.getPasswordHash())) {
                throw new AppException(ErrorCode.MATCH_PW);
            }
        }

        if (req.isWipeConvenienceData()) {
            oidMapper.deleteByUserId(userId);
        } else {
            oidMapper.deleteByUserId(userId);
        }

        String newLoginId = "deleted_" + userId;
        String newNickName = "탈퇴회원";
        String pwForDel = encodeRandomToBcrypt();

        int n = userMapper.softDeleteAndAnonymize(userId, newLoginId, newNickName, pwForDel, "DELETED");
        if (n != 1) throw new AppException(ErrorCode.DB_WRITE_FAILED, "탈퇴 처리에 실패했습니다.");
    }

    public String getNicknameByAccount(String account) {
        Optional<User> userOptional = userRepository.findByLoginId(account);
        return userOptional.map(User::getNickName).orElse(null);
    }

    private String encodeRandomToBcrypt() {
        byte[] buf = new byte[24];
        new SecureRandom().nextBytes(buf);
        String random = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        return passwordEncoder.encode("DELETED:" + random);
    }
}