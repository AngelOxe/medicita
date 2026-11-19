package com.medicita.dto.response;

import java.util.Map;

/** Numeros del dashboard (GET /api/dashboard/resumen). */
public record ResumenResponse(
        long citasHoy,
        long proximasConfirmadas,
        long pacientesActivos,
        long completadas,
        Map<String, Long> porEstado
) {
}
