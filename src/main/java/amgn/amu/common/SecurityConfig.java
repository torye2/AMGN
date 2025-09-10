package amgn.amu.common;

import amgn.amu.service.OauthBridgeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain oauth(HttpSecurity http, AuthenticationSuccessHandler success) throws Exception {
        http
                .securityMatcher("/oauth/**", "/login/oauth2/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .oauth2Login(o -> o.successHandler(success))
                .csrf(c -> c.disable());
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
            //bridge.upsertAndLogin(req, auth); // 아래 4) 참조
            res.sendRedirect("/"); // 필요 시 originalUrl로
        };
    }
}
