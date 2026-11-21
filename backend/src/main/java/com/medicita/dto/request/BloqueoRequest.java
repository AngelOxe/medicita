package com.medicita.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record BloqueoRequest(

        @NotNull(message = "El inicio es obligatorio")
        OffsetDateTime inicio,

        @NotNull(message = "El fin es obligatorio")
        OffsetDateTime fin,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 250, message = "El motivo admite hasta 250 caracteres")
        String motivo
) {

    public BloqueoRequest {
        motivo = motivo == null ? null : motivo.strip();
    }
}
