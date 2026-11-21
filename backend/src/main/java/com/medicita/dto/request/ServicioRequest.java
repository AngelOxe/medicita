package com.medicita.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicioRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripcion admite hasta 500 caracteres")
        String descripcion,

        @NotNull(message = "La duracion es obligatoria")
        @Positive(message = "La duracion debe ser mayor que cero")
        Integer duracionMin,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(
                integer = 8,
                fraction = 2,
                message = "El precio admite hasta 8 enteros y 2 decimales"
        )
        BigDecimal precio,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {

    public ServicioRequest {
        nombre = nombre == null ? null : nombre.strip();
        descripcion = descripcion == null ? null : descripcion.strip();
    }
}