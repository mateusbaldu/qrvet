package ipiranga.fatec.qrvet.dtos.response;

import java.time.Instant;

public record ActiveSessionResponse(String jti, Instant createdAt, Instant expiresAt) {}
