package com.medicita.dto.request;

import com.medicita.entity.EstadoCita;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Cuerpo para reservar. El backend calcula fin, precio y estado: el cliente no los envia.
 * pacienteId solo lo usan ADMIN y MEDICO (crean la cita de un paciente).
 */
public record CitaRequest(

        @NotNull(message = "Selecciona un servicio")
        Long servicioId,

        @NotNull(message = "Selecciona un horario")
        OffsetDateTime inicio,

        Long pacienteId
) {

    /** Cuerpo de PATCH /citas/{id}/estado: {"estado":"completada"} o {"estado":"no_asistio"}. */
    public record CambioEstado(

            @NotNull(message = "El estado es obligatorio")
            EstadoCita estado
    ) {
    }

    /** Filtro de "Mis citas": proximas (confirmadas futuras) o historial (todo lo demas). */
    public enum Tipo {
        proximas,
        historial
    }
}
