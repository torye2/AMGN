package amgn.amu.service;

import amgn.amu.domain.User;
import amgn.amu.dto.UpdateProfileRequest;
import amgn.amu.dto.UserProfileDto;
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
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    @Transactional
    public void updateProfile(String loginId, UpdateProfileRequest req) {
        User user = userRepository.findById(loginId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("정지된 계정입니다.");
        }

        user.setId(req.id());
        user.setEmail(req.email());
        user.setNickName(req.nickName());
        user.setPhoneNumber(req.phoneNumber());
        user.setBirthYear(req.birthYear());
        user.setBirthMonth(req.birthMonth());
        user.setBirthDay(req.birthDay());
        user.setProvince(req.province());
        user.setCity(req.city());
        user.setDetailAddress(req.detailAddress());

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
