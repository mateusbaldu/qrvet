package ipiranga.fatec.qrvet.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionServiceTests {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final SessionService service = new SessionService(redis);

    SessionServiceTests() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void registersSessionWithRefreshTokenTtl() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        service.registerSession(7L, "jti-1", createdAt);

        verify(values).set("sessao:usuario:7:jti-1", createdAt.toString(),
                java.time.Duration.ofDays(5));
    }

    @Test
    void validatesAndRevokesIndividualSession() {
        when(redis.hasKey("sessao:usuario:7:jti-1")).thenReturn(true);

        assertTrue(service.isSessionValid(7L, "jti-1"));
        service.revokeSession(7L, "jti-1");

        verify(redis).delete("sessao:usuario:7:jti-1");
    }

    @Test
    void treatsMissingSessionAsInvalidAndDeletesAllWhenKeysExist() {
        when(redis.hasKey("sessao:usuario:7:missing")).thenReturn(null);
        Set<String> keys = Set.of("sessao:usuario:7:a", "sessao:usuario:7:b");
        when(redis.keys("sessao:usuario:7:*")).thenReturn(keys);

        assertFalse(service.isSessionValid(7L, "missing"));
        service.revokeAllSessions(7L);

        verify(redis).delete(keys);
    }

    @Test
    void doesNotDeleteWhenThereAreNoSessions() {
        when(redis.keys("sessao:usuario:7:*")).thenReturn(Set.of());

        service.revokeAllSessions(7L);

        verify(redis, never()).delete(anySet());
    }

    @Test
    void listsSessionsAndParsesStoredCreationTime() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        when(redis.keys("sessao:usuario:7:*")).thenReturn(Set.of("sessao:usuario:7:jti-1"));
        when(values.get("sessao:usuario:7:jti-1")).thenReturn(createdAt.toString());
        when(redis.getExpire("sessao:usuario:7:jti-1", TimeUnit.SECONDS)).thenReturn(120L);

        var sessions = service.listSessions(7L);

        assertEquals(1, sessions.size());
        assertEquals("jti-1", sessions.get(0).jti());
        assertEquals(createdAt, sessions.get(0).createdAt());
        assertTrue(sessions.get(0).expiresAt().isAfter(Instant.now().plusSeconds(100)));
    }

    @Test
    void returnsEmptyListWhenRedisHasNoKeys() {
        when(redis.keys("sessao:usuario:7:*")).thenReturn(null);

        assertTrue(service.listSessions(7L).isEmpty());
    }
}
