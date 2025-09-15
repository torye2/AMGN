package amgn.amu.common;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireMfa {
    int maxAgeSeconds() default 1200;

    String reason() default "";
    String reasonCode() default "";
}
