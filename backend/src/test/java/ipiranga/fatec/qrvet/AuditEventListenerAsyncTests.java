package ipiranga.fatec.qrvet;

import ipiranga.fatec.qrvet.event.AuditEvent;
import ipiranga.fatec.qrvet.event.AuditEventListener;
import ipiranga.fatec.qrvet.repositories.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
class AuditEventListenerAsyncTests {
    @Autowired AuditEventListener listener;
    @MockitoBean AuditLogRepository auditLogs;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean JavaMailSender mail;

    @Test
    void returnsBeforeAuditPersistenceCompletes() throws Exception {
        CountDownLatch persistenceStarted = new CountDownLatch(1);
        CountDownLatch releasePersistence = new CountDownLatch(1);
        doAnswer(invocation -> {
            persistenceStarted.countDown();
            assertTrue(releasePersistence.await(2, TimeUnit.SECONDS));
            return invocation.getArgument(0);
        }).when(auditLogs).save(any());

        long startedAt = System.nanoTime();
        listener.handle(new AuditEvent(
                1L,
                "admin@qrvet.test",
                "127.0.0.1",
                "PATIENT_CREATED",
                "patient",
                "42",
                Map.of("name", "Luna")));

        assertTrue(persistenceStarted.await(2, TimeUnit.SECONDS));
        long elapsed = System.nanoTime() - startedAt;
        assertTrue(elapsed < Duration.ofSeconds(1).toNanos(),
                "The request thread waited for asynchronous audit persistence");
        releasePersistence.countDown();
    }
}
