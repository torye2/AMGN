package amgn.amu.dto.oauth_totp;

import lombok.Value;

import java.util.List;

@Value
public class BackupCodesResponse {
    List<String> backupCodesPlain; // 1회용 평문
}
