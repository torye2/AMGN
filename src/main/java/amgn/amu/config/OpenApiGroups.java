package amgn.amu.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroups {
    @Bean
    GroupedOpenApi portfolio() {
        return GroupedOpenApi.builder()
                .group("portfolio")
                .pathsToMatch("/signup/**", "/orders/**", "/api/listings/**", "/api/user/**", "/oauth/**", "/product/**","/payment/**", "/api/oauth/**", "/chat/**", "/api/**")
                .build();
    }
}
