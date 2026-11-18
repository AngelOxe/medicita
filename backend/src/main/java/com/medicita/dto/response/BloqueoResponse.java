package com.medicita.dto.response;

import com.medicita.entity.Bloqueo;

import java.time.OffsetDateTime;

public record BloqueoResponse(
        Long id,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        String motivo
) {

    public static BloqueoResponse desde(Bloqueo bloqueo) {
        return new BloqueoResponse(
                bloqueo.getId(),
                bloqueo.getInicio(),
                bloqueo.getFin(),
                bloqueo.getMotivo()
        );
    }
}
