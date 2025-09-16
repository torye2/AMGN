package amgn.amu.config;

import amgn.amu.service.util.UploadPathProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebImgConfig implements WebMvcConfigurer {

    private final UploadPathProvider uploadPathProvider;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String providerLocation = "file:" + uploadPathProvider.getUploadsDir().toString() + "/";
        // 필요 시 기존 경로(C:/amu/uploads)도 함께 서빙하여 호환 유지
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(providerLocation, "file:///C:/amu/uploads/");
    }
}
