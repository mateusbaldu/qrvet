package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.models.RecoveryData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class PasswordRecoveryService {
    private static final String PREFIX = "password-recovery:";
    private static final String COOLDOWN_PREFIX = "password-recovery-cooldown:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(20);
    private static final Duration COOLDOWN_TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;

    public PasswordRecoveryService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean canCreate(Long userId) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(COOLDOWN_PREFIX + userId, "1", COOLDOWN_TTL));
    }

    public void save(String token, Long userId, long authenticationVersion) {
        redis.opsForValue()
                .set(
                        key(token),
                        userId + ":" + authenticationVersion,
                        TOKEN_TTL);
    }

    public Optional<RecoveryData> consume(String token) {
        String value = redis.opsForValue().getAndDelete(key(token));

        if (value == null) return Optional.empty();

        String[] parts = value.split(":", -1);
        if (parts.length != 2) return Optional.empty();
        try {
            return Optional.of(new RecoveryData(Long.parseLong(parts[0]), Long.parseLong(parts[1])));
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
