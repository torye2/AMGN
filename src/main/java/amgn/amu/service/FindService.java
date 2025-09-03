package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.component.ResetTokenStore;
import amgn.amu.domain.User;
import amgn.amu.dto.FindIdRequest;
import amgn.amu.dto.FindIdResponse;
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


}
