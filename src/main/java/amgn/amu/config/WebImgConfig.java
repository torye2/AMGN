package amgn.amu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebImgConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 외부 폴더 C:/amu/uploads 를 /uploads/** URL로 접근 가능하게 설정
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///C:/amu/uploads/");
    }
}
