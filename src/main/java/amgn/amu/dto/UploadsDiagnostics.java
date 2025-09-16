package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadsDiagnostics {
    private String workingDir;
    private String uploadsAbsolutePath;
    private boolean exists;
    private boolean directory;
    private boolean createDirSucceeded;
    private boolean writable;
    private boolean probeWriteSucceeded;
    private String message;
    private String exception;
}
