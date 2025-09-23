package amgn.amu.common;

import amgn.amu.domain.User;
import amgn.amu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new UsernameNotFoundException(username + " user not found"));

        if (!"ACTIVE".equals(user.getStatus())) {
            if ("BANNED".equals(user.getStatus())) {
                throw new LockedException("banned");
            }
            throw new DisabledException("inactive/deleted");
        }

        return new CustomUserDetails(user);
    }
}
