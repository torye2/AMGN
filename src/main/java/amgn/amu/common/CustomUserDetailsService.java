package amgn.amu.common;

import amgn.amu.domain.User;
import amgn.amu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

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

        List<String> roleNames = jdbcTemplate.query(
                "SELECT role_name FROM user_roles WHERE user_id = ?",
                (rs, rn) -> rs.getString(1),
                user.getUserId()
        );

        List<GrantedAuthority> auths = new ArrayList<>();
        for (String r : roleNames) {
            auths.add(new SimpleGrantedAuthority(r));
        }

        if (auths.stream().noneMatch(a -> "ROLE_USER".equals(a.getAuthority()))) {
            auths.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new CustomUserDetails(user, auths);
    }
}
