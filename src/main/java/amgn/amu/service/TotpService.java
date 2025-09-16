package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.common.LoginUser;
import amgn.amu.dto.oauth_totp.BackupCodesResponse;
import amgn.amu.dto.oauth_totp.TotpSetupResponse;
import amgn.amu.entity.UserMfaBackupCode;
import amgn.amu.entity.UserMfaTotp;
import amgn.amu.mapper.UserMfaBackupCodeMapper;
import amgn.amu.mapper.UserMfaTotpMapper;
import amgn.amu.service.util.CryptoUtil;
import amgn.amu.service.util.QrUtil;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base32;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TotpService {
    private final UserMfaTotpMapper totpMapper;
    private final UserMfaBackupCodeMapper backupMapper;
    private final LoginUser loginUser;
    private final ProfileService profileService;

    private static final Base32 B32 = new Base32();
    private static final TimeBasedOneTimePasswordGenerator TOTP = new TimeBasedOneTimePasswordGenerator();

    public TotpSetupResponse beginSetup(long userId, String issuer, String accountName) {
        byte[] raw = new byte[20];
        new SecureRandom().nextBytes(raw);
        String base32 = B32.encodeToString(raw);

        byte[] enc = CryptoUtil.encrypt(base32.getBytes(StandardCharsets.UTF_8));
        totpMapper.upsert(userId, enc, false);

        String uri = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=6&period=30&algorithm=SHA1",
                url(issuer), url(accountName), base32, url(issuer)
        );
        String dataUrl = QrUtil.toDataUrlPng(uri);

        return new TotpSetupResponse(dataUrl, mask(base32));
    }

    public BackupCodesResponse activate(long userId, int code) throws Exception {
        ensureValid(userId, code);
        totpMapper.setEnabled(userId, true);

        List<String> plains = new ArrayList<>();
        List<UserMfaBackupCode> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String c = randomCode(8);
            plains.add(c);
            UserMfaBackupCode r = new UserMfaBackupCode();
            r.setCodeHash(BCrypt.hashpw(c, BCrypt.gensalt()));
            rows.add(r);
        }
        backupMapper.insertBatch(userId, rows);
        return new BackupCodesResponse(plains);
    }

    // 코드 검증
    public void ensureValid(long userId, int code) throws Exception {
        String base32 = loadBase32(userId);
        SecretKey key = new SecretKeySpec(B32.decode(base32), TOTP.getAlgorithm());
        Instant now = Instant.now();
        int cur = TOTP.generateOneTimePassword(key, now);
        int prev = TOTP.generateOneTimePassword(key, now.minus(TOTP.getTimeStep()));
        int next = TOTP.generateOneTimePassword(key, now.plus(TOTP.getTimeStep()));
        if (!(code == cur || code == prev || code == next)) {
            throw new AppException(ErrorCode.MFA_CODE_INVALID);
        }
    }

    public boolean verifyOrUseBackup(long userId, String input) throws Exception {
        if (input.matches("\\d{6}")) {
            try {
                ensureValid(userId, Integer.parseInt(input));
                return true;
            } catch (Exception ignore) {}
        }

        return backupMapper.listActive(userId).stream().anyMatch(b -> {
            if (BCrypt.checkpw(input, b.getCodeHash())) {
                backupMapper.markUsed(b.getBackupCodeId());
                return true;
            }
            return false;
        });
    }

    @Transactional
    public void disabled(long userId) {
        totpMapper.setEnabled(userId, false);
        backupMapper.revokeAll(userId);
    }

    public boolean isEnabled(long userId) {
        return totpMapper.get(userId).map(UserMfaTotp::isEnabled).orElse(false);
    }

    private String loadBase32(long userId) {
        var rec = totpMapper.get(userId).orElseThrow();
        byte[] dec = CryptoUtil.decrypt(rec.getSecretEnc());
        return new String(dec, StandardCharsets.UTF_8);
    }

    private static String url(String s) { return s.replace(" ", "%20"); }
    private static String mask(String s) {
        return (s.length() <= 4) ? "****" : s.substring(0, 4) + "****";
    }
    private static String randomCode(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
