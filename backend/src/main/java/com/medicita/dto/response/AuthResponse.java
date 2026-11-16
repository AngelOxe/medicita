package com.medicita.dto.response;

import java.time.Instant;

public record AuthResponse(
        String token,
        String tipo,
        Instant expiraEn,
        UsuarioResponse usuario
) {
}