package amgn.amu.config;

import amgn.amu.repository.UserRepository;
import amgn.amu.service.OauthBridgeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain oauth(HttpSecurity http,
                              AuthenticationSuccessHandler success,
                              AuthenticationFailureHandler failure) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**", "/api/oauth/**", "/logout", "/onboarding")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/csrf").permitAll()
                        .requestMatchers("/api/oauth/unlink", "/onboarding", "/api/onboarding").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(o -> o
                        .loginPage("/login")
                        .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                        .successHandler(success)
                        .failureHandler(failure)
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/logout"))
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/main"))
                .addFilterAfter(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                        if (token != null) { token.getToken(); }
                        filterChain.doFilter(request, response);
                    }
                }, CsrfFilter.class);
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
    AuthenticationSuccessHandler successHandler(OauthBridgeService bridge, UserRepository userRepository) {
        return (req, res, auth) -> {
            var result = bridge.upsertAndLogin(req, auth);
            Long uid = result.getLoginUser().getUserId();
            boolean needs = bridge.isOnboardingRequired(uid);
            res.sendRedirect(needs ? "/onboarding" : "/main");
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
