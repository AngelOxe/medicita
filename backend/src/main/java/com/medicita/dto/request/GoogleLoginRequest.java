package com.medicita.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/auth/google: el token (credential) que entrega Google al frontend. */
public record GoogleLoginRequest(

        @NotBlank(message = "Falta el token de Google")
        @Size(max = 4096, message = "El token de Google no es valido")
        String credential
) {
}
