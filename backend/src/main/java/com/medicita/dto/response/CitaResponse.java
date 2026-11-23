package com.medicita.dto.response;

import com.medicita.entity.Cita;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CitaResponse(
        Long id,
        PacienteResumen paciente,
        ServicioResumen servicio,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        BigDecimal precioReservado,
        String estado,
        OffsetDateTime creadoEn
) {

    public record PacienteResumen(Long id, String nombre) {
    }

    public record ServicioResumen(Long id, String nombre) {
    }

    public static CitaResponse desde(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                new PacienteResumen(cita.getPaciente().getId(), cita.getPaciente().getNombre()),
                new ServicioResumen(cita.getServicio().getId(), cita.getServicio().getNombre()),
                cita.getInicio(),
                cita.getFin(),
                cita.getPrecioReservado(),
                cita.getEstado(),
                cita.getCreadoEn()
        );
    }
}
