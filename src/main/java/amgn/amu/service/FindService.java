package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.component.ResetTokenStore;
import amgn.amu.domain.User;
import amgn.amu.dto.*;
import amgn.amu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FindService {
    private final UserRepository userRepository;
    private final ResetTokenStore tokenStore;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public FindIdResponse findId(FindIdRequest req) {
        Optional<User> findUser = userRepository.findByUserNameAndBirthYearAndBirthMonthAndBirthDayAndPhoneNumber(
                req.getUserName(), req.getBirthYear(), req.getBirthMonth(), req.getBirthDay(), req.getPhoneNumber()
        );

        User user = findUser.orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

        return new FindIdResponse(user.getId());
    }

    // 본인확인 → 토큰 발급
    @Transactional
    public ResetTokenResponse verifyAndToken(FindPwRequest req) {
        User user = userRepository.findByIdAndUserNameAndBirthYearAndBirthMonthAndBirthDayAndPhoneNumber(
                req.getId(), req.getUserName(), req.getBirthYear(), req.getBirthMonth(), req.getBirthDay(), req.getPhoneNumber()
        ).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

        String token = tokenStore.issue(user.getId(), 10 * 60);
        return new ResetTokenResponse(token);
    }

    // 커밋
    @Transactional
    public void resetPassword(ResetPwRequest req) {
        String loginId = tokenStore.consume(req.getToken());
        if(loginId == null) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
        }
        User user = userRepository.findById(loginId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

        String encodePw = passwordEncoder.encode(req.getNewPassword());
        user.setPasswordHash(encodePw);
    }
}
