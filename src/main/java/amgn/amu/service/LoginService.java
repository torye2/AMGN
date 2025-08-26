package amgn.amu.service;

import amgn.amu.domain.User;
import amgn.amu.dto.LoginRequest;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserMapper userMapper;

    public LoginUserDto login(LoginRequest req) {
        User user = userMapper.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
        if(!user.getPasswordHash().equals(req.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if(!user.getStatus().equals("ACTIVE")) {
            throw new IllegalArgumentException("휴면 계정입니다.");
        }
        return new LoginUserDto(user.getId(), user.getUserName()
                , user.getEmail(), user.getNickName(), user.getPhoneNumber()
                , user.getBirthYear(), user.getBirthMonth(), user.getBirthDay()
                , user.getProvince(), user.getCity(), user.getDetailAddress());
    }
}
