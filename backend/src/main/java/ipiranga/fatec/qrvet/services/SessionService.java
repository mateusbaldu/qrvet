package ipiranga.fatec.qrvet.services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class SessionService {
    private static final String PREFIX = "sessao:usuario:";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(5);

    private final StringRedisTemplate redis;

    public SessionService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void registerSession(Long userId, String jti, java.time.Instant createdAt) {
        redis.opsForValue()
                .set(
                        key(userId, jti),
                        createdAt.toString(),
                        REFRESH_TOKEN_TTL);
    }

    public boolean isSessionValid(Long userId, String jti) {
        return Boolean.TRUE.equals(redis.hasKey(key(userId, jti)));
    }

    public void revokeSession(Long userId, String jti) {
        redis.delete(key(userId, jti));
    }

    public void revokeAllSessions(Long userId) {
        Set<String> keys = redis.keys(PREFIX + userId + ":*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }

    public java.util.List<SessionData> listSessions(Long userId) {
        Set<String> keys = redis.keys(PREFIX + userId + ":*");
        if (keys == null) return java.util.List.of();
        return keys.stream()
                .map(key -> {
                    String jti = key.substring((PREFIX + userId + ":").length());
                    java.time.Instant createdAt = java.time.Instant.parse(redis.opsForValue().get(key));
                    Long expiresAt = redis.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
                    return new SessionData(jti, createdAt, java.time.Instant.now().plusSeconds(expiresAt));
                })
                .toList();
    }

    private String key(Long userId, String jti) {
        return PREFIX + userId + ":" + jti;
    }

    public record SessionData(String jti, java.time.Instant createdAt, java.time.Instant expiresAt) {}
}
