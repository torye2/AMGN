package amgn.amu.config;

import amgn.amu.component.LoginHelper;
import amgn.amu.domain.User;
import amgn.amu.dto.oauth_totp.PendingOauth;
import amgn.amu.repository.UserRepository;
import amgn.amu.service.OauthBridgeService;
import amgn.amu.service.util.OAuthProfileResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain oauth(HttpSecurity http,
                              AuthenticationSuccessHandler success,
                              AuthenticationFailureHandler failure) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**", "/api/oauth/**", "/logout")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/csrf", "/onboarding", "/api/onboarding").permitAll()
                        .requestMatchers("/api/oauth/unlink").authenticated()
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
    AuthenticationSuccessHandler successHandler(OauthBridgeService bridge,
                                                OAuth2AuthorizedClientService clientService,
                                                LoginHelper loginHelper) {
        return (req, res, auth) -> {
            var oauth = (OAuth2AuthenticationToken) auth;
            var provider = OAuthProfileResolver.providerOf(oauth);
            var attrs = oauth.getPrincipal().getAttributes();

            var pid = OAuthProfileResolver.pidOf(provider, attrs);
            var linked = bridge.findLinkedUserId(provider, pid);

            if (linked.isPresent()) {
                loginHelper.loginAs(req, res, linked.get(), null, null);
                res.sendRedirect("/main");
                return;
            }

            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(provider, oauth.getName());
            String accessToken = client.getAccessToken().getTokenValue();
            String refreshToken = client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null;
            var expriresAt = client.getAccessToken().getExpiresAt();

            PendingOauth po = new PendingOauth();
            po.setProvider(provider);
            po.setProviderUserId(pid);
            po.setEmail(OAuthProfileResolver.emailOf(provider, attrs));
            po.setEmailVerified(OAuthProfileResolver.emailVerifiedOf(provider, attrs));
            po.setDisplayName(OAuthProfileResolver.displayNameOf(provider, attrs));
            po.setAccessToken(accessToken);
            po.setRefreshToken(refreshToken);
            po.setTokenExpiresAt(expriresAt);

            req.getSession(true).setAttribute("PENDING_OAUTH", po);
            res.sendRedirect("/onboarding");
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
