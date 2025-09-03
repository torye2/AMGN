package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.domain.User;
import amgn.amu.dto.LoginRequest;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginUserDto login(LoginRequest req) {
        User user = userMapper.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (!(passwordEncoder.matches(req.getPasswordHash(), user.getPasswordHash())
            || user.getPasswordHash().equals(req.getPasswordHash()))) {
            throw new AppException(ErrorCode.MATCH_PW);
        }

        if(!user.getStatus().equals("ACTIVE")) {
            throw new IllegalArgumentException("정지된 계정입니다.");
        }
        return LoginUserDto.from(user);
    }
}
