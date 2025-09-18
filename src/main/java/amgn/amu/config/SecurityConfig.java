package amgn.amu.config;

import amgn.amu.component.LoginHelper;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.oauth_totp.PendingOauth;
import amgn.amu.repository.UserRepository;
import amgn.amu.service.OauthBridgeService;
import amgn.amu.service.util.OAuthProfileResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextHolderFilter;
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
                              AuthenticationFailureHandler failure,
                              LoginHelper loginHelper,
                              UserRepository userRepository) throws Exception {
        var repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookiePath("/");

        var handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName("_csrf");

        http
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(a -> a
                        // 공개 엔드포인트 화이트리스트
                        .requestMatchers(
                                "/", "/login", "/login.html", "/signup.html", "/main.html",
                                "/onboarding", "/onboarding.html","/productDetail.html",
                                "/css/**", "/js/**", "/img/**", "/favicon.ico",
                                "/search", "/category/**", "/footer.html", "/api/pw-reset/**",
                                "/api/csrf", "/header.html","/list.html", "/api/find-id",
                                "/oauth2/authorization/**","/login/oauth2/code/**"
                        ).permitAll()
                        // 읽기 전용 공개 API (HTTP GET만)
                        .requestMatchers(HttpMethod.GET,
                                "/faq", "/api/faqs/**", "/api/suggest",
                                "/api/categories/**", "/api/regions/**",
                                "/api/geo/**", "/uploads/**", "/api/users/**",
                                "/region/**", "/api/region/**", "/api/user/**",
                                "/api/listings/**", "/listing/**",
                                "/api/search/**", "/product/**", "/api/system/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/oauth/connect/**",
                                "/api/oauth/me",
                                "/api/oauth/link/confirm",
                                "/api/oauth/unlink",
                                "/myPage.html"
                        ).authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/onboarding").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(nextParamCaptureFilter(), OAuth2AuthorizationRequestRedirectFilter.class)
                .formLogin(f -> f
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .usernameParameter("id")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            var log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

                            log.info("LOGIN SUCCESS authName={}, principalClass={}",
                                    auth.getName(), auth.getPrincipal().getClass().getName());

                            var dto = loginHelper.fromAuthentication(auth); // Authentication → LoginUserDto
                            log.info("loginHelper dto = {}", dto);
                            if (dto == null) {
                                String loginId = auth.getName(); // usernameParameter("id")로 들어온 값
                                var userOpt = userRepository.findByLoginId(loginId);
                                if (userOpt.isPresent()) {
                                    var u = userOpt.get();
                                    // 프로젝트에 맞게 생성자/정적 팩토리 사용
                                    dto = LoginUserDto.from(u);
                                }
                            }
                            if (dto != null) {
                                req.getSession(true).setAttribute("loginUser", dto);
                                log.info("session set: loginUser.userId={}", dto.getUserId());
                            } else {
                                // 여기로 오면 뭔가 이상한 케이스 → 안전하게 로그인 페이지로 돌려보냄
                                res.sendRedirect("/login.html?error");
                                return;
                            }
                            var s = req.getSession(false);
                            String next = (s != null) ? (String) s.getAttribute("NEXT_URL") : null;
                            if (next != null) {
                                s.removeAttribute("NEXT_URL");
                                res.sendRedirect(next);
                            }
                            else { res.sendRedirect("/main.html"); }
                        })
                        .failureHandler((req, res, ex) -> {
                            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("AUTH");
                            log.warn("LOGIN FAIL: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
                            String msg = java.net.URLEncoder.encode("아이디 또는 비밀번호가 올바르지 않습니다.","UTF-8");
                            res.sendRedirect("/login.html?error=" + msg);
                        })
                        .failureUrl("/login.html?error")
                )
                .oauth2Login(o -> o
                        .loginPage("/login.html")
                        .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                        .successHandler(success)
                        .failureHandler(failure)
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(repo)
                        .csrfTokenRequestHandler(handler)
                        .ignoringRequestMatchers("/h2-console/**"))
                .addFilterAfter(csrfCookieFilter(), CsrfFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, ex2) -> {
                            if (isApi(req)) {
                                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                res.setContentType("application/json;charset=UTF-8");
                                res.getWriter().write("{\"error\":\"unauthorized\"}");
                            } else {
                                res.sendRedirect("/login.html");
                            }
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            if (isApi(req)) {
                                res.setContentType("application/json;charset=UTF-8");
                                res.getWriter().write("{\"error\":\"forbidden\"}");
                            } else {
                                res.setContentType("text/plain;charset=UTF-8");
                                res.getWriter().write("Forbidden");
                            }
                        })
                )
                .logout(l -> l
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/main.html")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID","XSRF-TOKEN")
                        .addLogoutHandler((req, res, auth) -> {
                            var s = req.getSession(false);
                            if (s != null) {
                                s.removeAttribute("PENDING_OAUTH");
                                s.removeAttribute("LINKING_PROVIDER");
                                s.removeAttribute("NEXT_URL");
                            }
                        })
                );
        return http.build();
    }

    @Bean
    AuthenticationSuccessHandler successHandler(OauthBridgeService bridge,
                                                OAuth2AuthorizedClientService clientService,
                                                LoginHelper loginHelper,
                                                UserRepository userRepository) {
        return (req, res, auth) -> {
            var session = req.getSession(false);
            var oauth = (OAuth2AuthenticationToken) auth;
            var provider = OAuthProfileResolver.providerOf(oauth).toLowerCase();
            var attrs = oauth.getPrincipal().getAttributes();
            var pid = OAuthProfileResolver.pidOf(provider, attrs);

            var client = clientService.loadAuthorizedClient(provider, oauth.getName());
            String accessToken = client.getAccessToken().getTokenValue();
            String refreshToken = client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null;
            var expiresAt = client.getAccessToken().getExpiresAt();

            var po = new PendingOauth();
            po.setProvider(provider);
            po.setProviderUserId(pid);
            po.setEmail(OAuthProfileResolver.emailOf(provider, attrs));
            po.setEmailVerified(OAuthProfileResolver.emailVerifiedOf(provider, attrs));
            po.setDisplayName(OAuthProfileResolver.displayNameOf(provider, attrs));
            po.setAccessToken(accessToken);
            po.setRefreshToken(refreshToken);
            po.setTokenExpiresAt(expiresAt);

            String linkingProvider = (session != null) ? (String) session.getAttribute("LINKING_PROVIDER") : null;
            Long linkingUserId = (session != null) ? (Long) session.getAttribute("LINK_USER_ID") : null;

            if (linkingProvider != null && linkingProvider.equals(provider) && linkingUserId != null) {
                var linked = bridge.findLinkedUserId(provider, pid);

                if (linked.isPresent() && !linked.get().equals(linkingUserId)) {
                    session.removeAttribute("LINK_USER_ID");
                    session.removeAttribute("LINKING_PROVIDER");
                    session.setAttribute("FLASH_MSG", "이미 다른 계정에 연결된 소셜 계정입니다.");
                    res.sendRedirect("/myPage.html#account");
                    return;
                }

                if (linked.isEmpty()) {
                    bridge.linkIdentity(linkingUserId, po);
                    session.setAttribute("FLASH_MSG", "소셜 계정 연동에 성공했습니다.");
                }

                loginHelper.loginAs(req, res, linkingUserId, null, null);
                var dto = (LoginUserDto) req.getSession(true).getAttribute("loginUser");

                if (dto == null) {
                    dto = LoginUserDto.from(userRepository.findByUserId(linkingUserId).get());
                    req.getSession(true).setAttribute("loginUser", dto);
                }

                session.removeAttribute("LINK_USER_ID");
                session.removeAttribute("LINKING_PROVIDER");
                String back = (String) (session.getAttribute("LINK_RETURN") != null
                        ? session.getAttribute("LINK_RETURN") : "/myPage.html#account");
                session.removeAttribute("LINK_RETURN");
                res.sendRedirect(back);
                return;
            }

            var linked = bridge.findLinkedUserId(provider, pid);
            if (linked.isPresent()) {
                loginHelper.loginAs(req, res, linked.get(), null, null);
                var dto = (LoginUserDto) req.getSession(true).getAttribute("loginUser");

                if (dto == null) {
                    dto = LoginUserDto.from(userRepository.findByUserId(linked.get()).get());
                    req.getSession(true).setAttribute("loginUser", dto);
                }
                res.sendRedirect("/main.html");
                return;
            }

            req.getSession(true).setAttribute("PENDING_OAUTH", po);

            String next = session != null ? (String) session.getAttribute("NEXT_URL") : null;
            if (next != null) {
                session.removeAttribute("NEXT_URL");
                res.sendRedirect(next);
            } else {
                res.sendRedirect("/onboarding");
            }
        };
    }

    @Bean
    AuthenticationFailureHandler failureHandler() {
        return (req, res, ex) -> {
            var session = req.getSession(false);
            String back = (session != null) ? (String) session.getAttribute("LINK_RETURN") : null;
            if (back != null) {
                session.setAttribute("FLASH_MSG", "소셜 계정 연결 실패: " + ex.getMessage());
                session.removeAttribute("LINK_RETURN");
                res.sendRedirect(back);
                return;
            }
            String msg = java.net.URLEncoder.encode("소셜 로그인 실패: " + ex.getMessage(),
                    java.nio.charset.StandardCharsets.UTF_8);
            res.sendRedirect("/login.html?error=" + msg);
        };
    }

    @Bean
    OncePerRequestFilter nextParamCaptureFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                String next = req.getParameter("next");
                if (next != null && !next.isBlank()
                        && next.startsWith("/") && !next.startsWith("//")
                        && !next.equals("/login") && !next.equals("/login.html")) {
                    req.getSession(true).setAttribute("NEXT_URL", next);
                }
                chain.doFilter(req, res);
            }
        };
    }
    @Bean
    OncePerRequestFilter csrfCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                CsrfToken token = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
                if (token == null) {
                    token = (CsrfToken) req.getAttribute("_csrf"); // 보수적으로 둘 다 확인
                }
                if (token != null) {
                    String val = token.getToken();
                    Cookie[] cookies = req.getCookies();
                    String existing = null;
                    if (cookies != null) {
                        for (var c : cookies) {
                            if ("XSRF-TOKEN".equals(c.getName())) { existing = c.getValue(); break; }
                        }
                    }
                    if (existing == null || !existing.equals(val)) {
                        var c = new Cookie("XSRF-TOKEN", val);
                        c.setPath("/");
                        c.setHttpOnly(false);                 // JS에서 읽게
                        c.setSecure(req.isSecure());          // HTTPS면 true
                        c.setMaxAge(-1);
                        res.addCookie(c);                     // ★ 실제 쿠키 쓰기
                    }
                }
                chain.doFilter(req, res);
            }
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByLoginId(username)
                .map(u -> User
                        .withUsername(u.getLoginId())
                        .password(u.getPasswordHash())
                        .roles("USER")
                        .accountExpired(false).accountLocked(false)
                        .credentialsExpired(false).disabled(false)
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    private boolean isApi(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String accept = req.getHeader("Accept");
        String xr = req.getHeader("X-Requested-With");
        return uri.startsWith("/api/")
                || uri.startsWith("/orders")
                || uri.startsWith("/product")
                || uri.startsWith("/listing")
                || uri.startsWith("/review")
                || (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(xr);
    }
}
