package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.domain.User;
import amgn.amu.dto.UpdateProfileRequest;
import amgn.amu.dto.UserProfileDto;
import amgn.amu.mapper.UserMapper;
import amgn.amu.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserMapper userMapper;

    public UserProfileDto getProfile(String loginId) {
        User user = userRepository.findById(loginId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("정지된 계정입니다.");
        }
        return maptoDto(user);
    }

    public boolean verifyPassword(String loginId, String password) {
        User user = userRepository.findById(loginId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return passwordEncoder.matches(password, user.getPasswordHash())
                || user.getPasswordHash().equals(password);
    }

    @Transactional
    public void updateProfile(String loginId, UpdateProfileRequest req) {
        User user = userRepository.findById(loginId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("정지된 계정입니다.");
        }

        if(req.id()!=null && !req.id().equals(user.getId())){
            if(userMapper.existsById(req.id())) throw new AppException(ErrorCode.DUPLICATE_ID);
            user.setId(req.id());
        }

        if(req.email()!=null) user.setEmail(req.email());
        if(req.phoneNumber()!=null) user.setPhoneNumber(req.phoneNumber());
        if(req.nickName()!=null) user.setNickName(req.nickName());
        if(req.birthYear()!=null) user.setBirthYear(req.birthYear());
        if(req.birthMonth()!=null) user.setBirthMonth(req.birthMonth());
        if(req.birthDay()!=null) user.setBirthDay(req.birthDay());
        if(req.province()!=null) user.setProvince(req.province());
        if(req.city()!=null) user.setCity(req.city());
        if(req.detailAddress()!=null) user.setDetailAddress(req.detailAddress());

        // 비밀번호 변경 (선택)
        if(req.newPassword()!=null && !req.newPassword().isBlank()){
            if (!(passwordEncoder.matches(req.newPassword(), user.getPasswordHash())
                    || user.getPasswordHash().equals(req.newPassword()))) {
                user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
            } else {
                throw new AppException(ErrorCode.DUPLICATE_PW);
            }
        }

        userRepository.save(user);
    }

    public UserProfileDto maptoDto(User user) {
        return new UserProfileDto(
                user.getId(), user.getUserName(), user.getEmail()
                , user.getNickName(), user.getGender(), user.getPhoneNumber()
                , user.getBirthYear(), user.getBirthMonth(), user.getBirthDay()
                , user.getProvince(), user.getCity(), user.getDetailAddress()
        );
    }
}
