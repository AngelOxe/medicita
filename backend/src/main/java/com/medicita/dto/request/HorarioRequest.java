package com.medicita.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.Set;

public record HorarioRequest(

        @NotNull(message = "El dia es obligatorio")
        @Min(value = 1, message = "El dia debe estar entre 1 (lunes) y 7 (domingo)")
        @Max(value = 7, message = "El dia debe estar entre 1 (lunes) y 7 (domingo)")
        Integer diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo,

        @NotEmpty(message = "Selecciona al menos un servicio")
        Set<Long> servicioIds
) {
}
