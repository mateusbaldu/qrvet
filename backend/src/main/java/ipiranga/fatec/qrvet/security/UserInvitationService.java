package ipiranga.fatec.qrvet.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

import static java.lang.Boolean.TRUE;

@Service
public class UserInvitationService {
    private static final String PREFIX = "invitation:";
    private static final String COOLDOWN_PREFIX = "invitation:reenviado:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private static final Duration COOLDOWN_TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;

    public UserInvitationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean canSend(Long userId) {
        return TRUE.equals(
                redis.opsForValue().setIfAbsent(COOLDOWN_PREFIX + userId, "1", COOLDOWN_TTL));
    }

    public void save(String token, Long userId) {
        redis.opsForValue()
                .set(
                        key(token),
                        userId.toString(),
                        TOKEN_TTL);
    }

    public Optional<Long> consume(String token) {
        String value = redis.opsForValue().getAndDelete(key(token));

        if (value == null) return Optional.empty();

        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public void delete(String token) {
        redis.delete(key(token));
    }

    private String key(String token) {
        return PREFIX + token;
    }
}
