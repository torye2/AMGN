package amgn.amu.service.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class UploadPathProvider {

    private final Path uploadsDir;

    public UploadPathProvider(@Value("${app.uploads.path:}") String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            this.uploadsDir = Path.of(System.getProperty("user.dir"), "uploads")
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
