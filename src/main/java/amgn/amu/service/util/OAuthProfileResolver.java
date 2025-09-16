package amgn.amu.service.util;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unchecked")
public class OAuthProfileResolver {

    public static String providerOf(OAuth2AuthenticationToken auth) {
        return auth.getAuthorizedClientRegistrationId().toLowerCase(Locale.ROOT); // "google","kakao","naver"
    }

    public static String pidOf(String provider, Map<String,Object> a) {
        return switch (provider) {
            case "google" -> str(a.get("sub"));
            case "kakao"  -> str(a.get("id")); // Long로 올 때가 있어 String으로 캐스팅 금지
            case "naver"  -> str(map(a.get("response")).get("id"));
            default       -> throw new IllegalStateException("Unsupported provider: " + provider);
        };
    }

    public static String emailOf(String provider, Map<String,Object> a) {
        return switch (provider) {
            case "google" -> str(a.get("email"));
            case "kakao"  -> str(map(a.get("kakao_account")).get("email")); // scope 동의 필요
            case "naver"  -> str(map(a.get("response")).get("email"));
            default       -> null;
        };
    }

    public static boolean emailVerifiedOf(String provider, Map<String,Object> a) {
        return switch (provider) {
            case "google" -> bool(a.get("email_verified"));
            case "kakao"  -> bool(map(a.get("kakao_account")).get("is_email_verified"));
            case "naver"  -> true; // 네이버는 별도 플래그 없음: 필요시 추가 검증 로직
            default       -> false;
        };
    }

    public static String displayNameOf(String provider, Map<String,Object> a) {
        return switch (provider) {
            case "google" -> firstNonBlank(str(a.get("name")),
                    (str(a.get("given_name")) + " " + str(a.get("family_name"))).trim());
            case "kakao"  -> {
                Map<String,Object> ka = map(a.get("kakao_account"));
                Map<String,Object> profile = map(ka.get("profile"));
                String nick = str(profile.get("nickname"));
                if (isBlank(nick)) nick = str(map(a.get("properties")).get("nickname"));
                yield nick;
            }
            case "naver"  -> {
                Map<String,Object> r = map(a.get("response"));
                String name = str(r.get("name"));
                if (isBlank(name)) name = str(r.get("nickname"));
                yield name;
            }
            default -> null;
        };
    }

    // helpers
    private static Map<String,Object> map(Object o) { return o instanceof Map ? (Map<String,Object>) o : Map.of(); }
    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static boolean bool(Object o) { return (o instanceof Boolean b) ? b : "true".equalsIgnoreCase(str(o)); }
    private static boolean isBlank(String s){ return s == null || s.isBlank(); }
    private static String firstNonBlank(String... ss){ for (var s:ss) if(!isBlank(s)) return s; return null; }

}
