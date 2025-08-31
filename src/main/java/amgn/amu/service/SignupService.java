package amgn.amu.service;

import amgn.amu.domain.User;
import amgn.amu.dto.SignupDto;
import amgn.amu.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void regist(SignupDto dto) {
        if (userMapper.existsById(dto.getId())) {
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
        user.setId(dto.getId());
        user.setPasswordHash(passwordHash);
        user.setUserName(dto.getUserName());
        user.setEmail(dto.getEmail());
        user.setNickName(dto.getNickName());
        user.setGender(dto.getGender());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setBirthYear(dto.getBirthYear());
        user.setBirthMonth(dto.getBirthMonth());
        user.setBirthDay(dto.getBirthDay());
        user.setProvince(dto.getProvince());
        user.setCity(dto.getCity());
        user.setDetailAddress(dto.getDetailAddress());

        userMapper.insert(user);
    }
}
