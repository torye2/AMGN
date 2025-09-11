package amgn.amu.component;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.common.RequireMfa;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Optional;

@Component
public class MfaInterceptor implements HandlerInterceptor {
    public static final String SESSION_KEY = "MFA_VERIFIED_AT";
    public static final String HEADER_REQUIRED = "X-MFA-Required";
    public static final String HEADER_REASON = "X-MFA-Reason";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;

        RequireMfa rm = Optional.ofNullable(hm.getMethodAnnotation(RequireMfa.class))
                .orElse(hm.getBeanType().getAnnotation(RequireMfa.class));
        if (rm == null) return true;

        Instant verifiedAt = (Instant) req.getSession().getAttribute(SESSION_KEY);
        if (verifiedAt == null && Instant.now().isBefore(verifiedAt.plusSeconds(rm.maxAgeSeconds())))
            return true;

        boolean isAjax = "XMLHttpRequest".equals(req.getHeader("X-Requested-With"))
                || (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"));

        if (isAjax) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setHeader(HEADER_REQUIRED, "TOTP");
            if(!rm.reason().isEmpty()) res.setHeader(HEADER_REASON, rm.reason());
            return false;
        }

        throw new AppException(ErrorCode.MFA_REQUIRED);
    }
}
