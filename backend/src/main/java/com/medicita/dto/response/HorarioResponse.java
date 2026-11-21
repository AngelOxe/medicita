package com.medicita.dto.response;

import com.medicita.entity.Horario;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

public record HorarioResponse(
        Long id,
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean activo,
        List<ServicioResumen> servicios
) {

    /** Servicio mostrado dentro de un horario: solo id y nombre. */
    public record ServicioResumen(Long id, String nombre) {
    }

    public static HorarioResponse desde(Horario horario) {
        List<ServicioResumen> servicios = horario.getServicios().stream()
                .map(s -> new ServicioResumen(s.getId(), s.getNombre()))
                .sorted(Comparator.comparing(ServicioResumen::nombre))
                .toList();

        return new HorarioResponse(
                horario.getId(),
                horario.getDiaSemana().intValue(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.isActivo(),
                servicios
        );
    }
}
