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
    private static final byte[] KEY = Base64.getDecoder().decode(System.getenv("MFA_AES_KEY"));

    public static byte[] encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[12]; RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance(TRANS);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, ALG), new GCMParameterSpec(TAG, iv));
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
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, ALG), new GCMParameterSpec(TAG, iv));
            return c.doFinal(ct);
        } catch (Exception e) {
            throw new AppException(ErrorCode.MFA_CRYPTO_ERROR, e);
        }
    }
}
