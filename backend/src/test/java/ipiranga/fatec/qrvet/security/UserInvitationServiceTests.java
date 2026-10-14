package ipiranga.fatec.qrvet.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserInvitationServiceTests {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final UserInvitationService service = new UserInvitationService(redis);

    UserInvitationServiceTests() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void allowsSendingOnlyWhenCooldownKeyIsCreated() {
        when(values.setIfAbsent("invitation:reenviado:12", "1", Duration.ofMinutes(1)))
                .thenReturn(true, false);

        assertTrue(service.canSend(12L));
        assertFalse(service.canSend(12L));
    }

    @Test
    void savesAndConsumesInvitationToken() {
        when(values.getAndDelete("invitation:abc")).thenReturn("12");

        service.save("abc", 12L);
        assertEquals(12L, service.consume("abc").orElseThrow());

        verify(values).set("invitation:abc", "12", Duration.ofHours(24));
    }

    @Test
    void ignoresMissingOrMalformedInvitationTokens() {
        when(values.getAndDelete("invitation:missing")).thenReturn(null);
        when(values.getAndDelete("invitation:bad")).thenReturn("not-a-number");

        assertTrue(service.consume("missing").isEmpty());
        assertTrue(service.consume("bad").isEmpty());
    }

    @Test
    void deletesInvitationToken() {
        service.delete("abc");

        verify(redis).delete("invitation:abc");
    }
}
