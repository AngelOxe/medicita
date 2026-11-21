package com.medicita.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DisponibilidadResponse(
        LocalDate fecha,
        Long servicioId,
        String zonaHoraria,
        List<Slot> slots
) {

    /** Un horario libre: el frontend envia exactamente este inicio al reservar. */
    public record Slot(OffsetDateTime inicio, OffsetDateTime fin) {
    }
}
