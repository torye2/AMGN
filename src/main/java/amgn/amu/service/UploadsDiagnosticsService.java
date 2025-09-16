package amgn.amu.service;

import amgn.amu.dto.UploadsDiagnostics;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class UploadsDiagnosticsService {

    public UploadsDiagnostics diagnose() {
        String workingDir = System.getProperty("user.dir");
        Path uploads = Path.of("uploads").toAbsolutePath().normalize();

        boolean exists = false;
        boolean directory = false;
        boolean createDirSucceeded = false;
        boolean writable = false;
        boolean probeWriteSucceeded = false;
        String message = null;
        String exception = null;

        try {
            exists = Files.exists(uploads);
            if (!exists) {
                Files.createDirectories(uploads);
                exists = true;
                createDirSucceeded = true;
            } else {
                createDirSucceeded = true; // 이미 존재해도 생성 성공으로 간주
            }

            directory = Files.isDirectory(uploads);
            writable = Files.isWritable(uploads);

            // 실제 쓰기 테스트
            var test = uploads.resolve(".permtest_" + UUID.randomUUID() + ".tmp");
            try {
                Files.writeString(test, "ok");
                probeWriteSucceeded = true;
            } finally {
                try {
                    Files.deleteIfExists(test);
                } catch (Exception ignore) {
                }
            }

            message = "OK";
        } catch (Exception e) {
            message = "uploads 경로 생성 또는 쓰기 테스트 중 오류가 발생했습니다.";
            exception = e.getClass().getName() + ": " + e.getMessage();
        }

        return new UploadsDiagnostics(
                workingDir,
                uploads.toString(),
                exists,
                directory,
                createDirSucceeded,
                writable,
                probeWriteSucceeded,
                message,
                exception
        );
    }
}
