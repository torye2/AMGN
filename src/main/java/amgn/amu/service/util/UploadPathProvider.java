package amgn.amu.service.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class UploadPathProvider {

    private final Path uploadsDir;

    public UploadPathProvider(@Value("${app.uploads.path:}") String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            // 기본값을 프로젝트 내부가 아닌 외부 디렉터리(C:/amu/uploads)로 설정
            this.uploadsDir = Path.of("C:/amu/uploads")
                    .toAbsolutePath()
                    .normalize();
        } else {
            this.uploadsDir = Path.of(configuredPath)
                    .toAbsolutePath()
                    .normalize();
        }
    }

    public Path getUploadsDir() {
        return uploadsDir;
    }
}
