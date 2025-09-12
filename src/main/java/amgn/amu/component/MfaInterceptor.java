package amgn.amu.component;

import amgn.amu.common.RequireMfa;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

@Component
public class MfaInterceptor implements HandlerInterceptor {
    public static final String SESSION_KEY = "MFA_VERIFIED_AT";
    public static final String SESSION_EXPECTED_REASON = "MFA_EXPECTED_REASON";
    public static final String SESSION_VERIFIED_REASON = "MFA_VERIFIED_REASON";
    public static final String HEADER_REQUIRED = "X-MFA-Required";
    public static final String HEADER_REASON = "X-MFA-Reason";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod hm)) return true;

        RequireMfa ann = Optional.ofNullable(hm.getMethodAnnotation(RequireMfa.class))
                .orElse(hm.getBeanType().getAnnotation(RequireMfa.class));
        if (ann == null) return true;

        var session = req.getSession(false);
        java.time.Instant verifiedAt = null;
        if (session != null) {
            Object attr = session.getAttribute(SESSION_KEY);
            if (attr instanceof java.time.Instant) verifiedAt = (java.time.Instant) attr;
        }

        int maxAge = ann.maxAgeSeconds();               // ← 얼마로 들어오는지 보자
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant until = (verifiedAt == null) ? null : verifiedAt.plusSeconds(maxAge);
        boolean within = (verifiedAt != null) && now.isBefore(until);
        String expectedReason = "PAY_EXEC";
        // 만약 @RequireMfa(reasonCode="...") 를 쓰고 있다면: String expectedReason = ann.reasonCode();

        if (within) {
            // 이미 시간 조건은 통과 → 이유까지 검사하려면 VERIFIED_REASON과 비교
            String verifiedReason = (session == null) ? null : (String) session.getAttribute(SESSION_VERIFIED_REASON);
            boolean sameReason = expectedReason == null || expectedReason.equals(verifiedReason);

            System.out.println("[MFA] maxAge=" + maxAge + " now=" + now +
                    " verifiedAt=" + verifiedAt + " until=" + until +
                    " within=" + within + " sameReason=" + sameReason +
                    " expected=" + expectedReason + " verifiedReason=" + verifiedReason +
                    " sid=" + (session==null? "null" : session.getId()));

            if (sameReason) return true;
        } else {
            System.out.println("[MFA] maxAge=" + maxAge + " now=" + now +
                    " verifiedAt=" + verifiedAt + " within=false sid=" + (session==null? "null" : session.getId()));
        }

        // ★ 차단 직전에 "기대 이유"를 세션에 심어둔다 (verify가 이 값을 복사해 감)
        if (session != null) {
            session.setAttribute(SESSION_EXPECTED_REASON, expectedReason);
        }

        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setHeader(HEADER_REQUIRED, "TOTP");
        res.setHeader(HEADER_REASON, expectedReason);  // ASCII만
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":\"MFA_REQUIRED\",\"reasonId\":\""+ expectedReason +"\"}");
        res.flushBuffer();
        return false;
    }
}

