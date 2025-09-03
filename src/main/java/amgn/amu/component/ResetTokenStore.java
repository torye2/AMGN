package amgn.amu.component;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ResetTokenStore {
    private static class Entry {
        final String loginId;
        final Instant exp;
        Entry(String loginId, Instant exp) { this.loginId = loginId; this.exp = exp; }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public String issue(String loginId, long ttlSeconds) {
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, new Entry(loginId, Instant.now().plusSeconds(ttlSeconds)));
        return token;
    }

    public String consume(String token) {
        Entry e = store.remove(token);              // 1회성
        if (e == null) return null;
        if (Instant.now().isAfter(e.exp)) return null;
        return e.loginId;
    }
}
