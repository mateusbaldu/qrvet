package ipiranga.fatec.qrvet.security;

import ipiranga.fatec.qrvet.dtos.RecoveryData;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordRecoveryServiceTests {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final PasswordRecoveryService service = new PasswordRecoveryService(redis);

    PasswordRecoveryServiceTests() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void createsCooldownOnlyWhenRedisAcceptsIt() {
        when(values.setIfAbsent(eq("password-recovery-cooldown:9"), eq("1"), any(Duration.class)))
                .thenReturn(true, false);

        assertTrue(service.canCreate(9L));
        assertFalse(service.canCreate(9L));
    }

    @Test
    void savesAndConsumesValidRecoveryData() {
        when(values.getAndDelete("password-recovery:token")).thenReturn("9:4");

        service.save("token", 9L, 4L);
        Optional<RecoveryData> result = service.consume("token");

        verify(values).set("password-recovery:token", "9:4", Duration.ofMinutes(20));
        assertEquals(Optional.of(new RecoveryData(9L, 4L)), result);
    }

    @Test
    void rejectsMissingAndMalformedRecoveryData() {
        when(values.getAndDelete("password-recovery:missing")).thenReturn(null);
        when(values.getAndDelete("password-recovery:bad")).thenReturn("9");
        when(values.getAndDelete("password-recovery:invalid")).thenReturn("x:y");

        assertTrue(service.consume("missing").isEmpty());
        assertTrue(service.consume("bad").isEmpty());
        assertTrue(service.consume("invalid").isEmpty());
    }

    @Test
    void deletesRecoveryToken() {
        service.delete("token");

        verify(redis).delete("password-recovery:token");
    }
}
