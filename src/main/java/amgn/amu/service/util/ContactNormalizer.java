package amgn.amu.service.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.text.Normalizer;
import java.util.Locale;

public class ContactNormalizer {
    private static final PhoneNumberUtil PNU = PhoneNumberUtil.getInstance();

    public ContactNormalizer() {}

    public static String normalizeEmail(String raw) {
        if (raw == null) return null;
        return Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }

    /** @param defaultRegion 예: "KR" */
    public static String toE164(String raw, String defaultRegion) {
        if (raw == null || raw.isBlank()) return null;
        try {
            var num = PNU.parse(raw, defaultRegion);
            if (!PNU.isValidNumber(num)) return null;
            return PNU.format(num, PhoneNumberUtil.PhoneNumberFormat.E164); // +821012345678
        } catch (NumberParseException e) {
            return null;
        }
    }
}
