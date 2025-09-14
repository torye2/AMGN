package amgn.amu.config;

import amgn.amu.service.OauthBridgeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain oauth(HttpSecurity http,
                              AuthenticationSuccessHandler success,
                              AuthenticationFailureHandler failure) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**", "/api/oauth/**", "/logout")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .oauth2Login(o -> o
                        .loginPage("/login")
                        .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                        .successHandler(success)
                        .failureHandler(failure)
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"))
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/main"));
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain rest(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(c -> c.disable());
        return http.build();
    }

    @Bean
    AuthenticationSuccessHandler successHandler(OauthBridgeService bridge) {
        return (req, res, auth) -> {
            bridge.upsertAndLogin(req, auth);
            res.sendRedirect("/main");
        };
    }

    @Bean
    AuthenticationFailureHandler failureHandler() {
        return (req, res, ex) -> {
            String msg = java.net.URLEncoder.encode("소셜 로그인 실패: " + ex.getMessage(),
                    java.nio.charset.StandardCharsets.UTF_8);
            res.sendRedirect("/login?error=" + msg);
        };
    }
}
