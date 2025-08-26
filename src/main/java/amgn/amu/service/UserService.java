package amgn.amu.service;

import amgn.amu.domain.User;
import amgn.amu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean login(String account, String password) {
        // 데이터베이스에서 아이디로 사용자를 찾음
        Optional<User> userOptional = userRepository.findById(account);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 사용자가 존재하면 입력된 비밀번호와 데이터베이스의 비밀번호를 비교함
            if (user.getPasswordHash().equals(password)) {
                return true; // 비밀번호 일치, 로그인 성공
            }
        }
        return false; // 사용자 없음 또는 비밀번호 불일치, 로그인 실패
    }

    public String getNicknameByAccount(String account) {
        Optional<User> userOptional = userRepository.findById(account);
        return userOptional.map(User::getNickName).orElse(null);
    }
}