package ipiranga.fatec.qrvet.dtos;

import ipiranga.fatec.qrvet.dtos.response.AccessTokenResponse;

public record AuthSession(AccessTokenResponse tokens, String refreshToken) {}

