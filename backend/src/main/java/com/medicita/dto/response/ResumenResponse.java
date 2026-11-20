package com.medicita.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Numeros del dashboard (GET /api/dashboard/resumen). */
public record ResumenResponse(
        long citasHoy,
        long proximasConfirmadas,
        long pacientesActivos,
        long completadas,
        Map<String, Long> porEstado,
        List<Dia> semana
) {

    /** Citas no canceladas de un dia de la semana en curso (lunes a domingo). */
    public record Dia(LocalDate fecha, long total) {
    }
}
