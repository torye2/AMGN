package amgn.amu.service.util;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtil {
    private static final String ALG = "AES";
    private static final String TRANS = "AES/GCM/NoPadding";
    private static final int TAG = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile SecretKeySpec KEY;

    private static SecretKeySpec key() {
        if (KEY != null) return KEY;
        synchronized (CryptoUtil.class) {
            if (KEY == null) {
                try {
                    String raw = System.getenv("MFA_AES_KEY");
                    if (raw == null || raw.isBlank())
                        throw new AppException(ErrorCode.SECURITY_MISSING_ENV, "MFA_AES_KEY is missing");

                    String b64 = raw.trim();
                    if (b64.startsWith("\"") && b64.endsWith("\"")) {
                        b64 = b64.substring(1, b64.length()-1);
                    }

                    byte[] keyBytes;
                    try {
                        keyBytes = Base64.getDecoder().decode(b64);
                    } catch (IllegalArgumentException iae) {
                        throw new AppException(ErrorCode.SECURITY_MISSING_ENV, "MFA_AES_KEY is not valid Base64", iae);
                    }
                    if (keyBytes.length != 32) {
                        throw new AppException(ErrorCode.SECURITY_MISSING_ENV, "MFA_AES_KEY must decode to 32 bytes, got="+keyBytes.length);
                    }
                    KEY = new SecretKeySpec(keyBytes, "AES");
                } catch (AppException ae) {
                    throw ae; // 우리 커스텀 코드 그대로 던짐
                } catch (Exception e) {
                    throw new AppException(ErrorCode.MFA_CRYPTO_ERROR, "Failed to load MFA key", e);
                }
            }
        }
        return KEY;
    }

    public static byte[] encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[12]; RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance(TRANS);
            c.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG, iv));
            byte[] ct = c.doFinal(plain);
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return out;
        } catch (Exception e) {
            throw new AppException(ErrorCode.MFA_CRYPTO_ERROR, e);
        }
    }
    public static byte[] decrypt(byte[] enc) {
        try {
            byte[] iv= new byte[12];
            System.arraycopy(enc, 0, iv, 0, 12);
            byte[] ct = new byte[enc.length - 12];
            System.arraycopy(enc, 12, ct, 0, ct.length);
            Cipher c = Cipher.getInstance(TRANS);
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG, iv));
            return c.doFinal(ct);
        } catch (Exception e) {
            throw new AppException(ErrorCode.MFA_CRYPTO_ERROR, e);
        }
    }
}
