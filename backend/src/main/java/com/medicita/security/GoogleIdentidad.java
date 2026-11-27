package com.medicita.security;

/** Datos de la cuenta de Google, ya verificados con la firma de Google. */
public record GoogleIdentidad(
        String sub,
        String email,
        boolean emailVerificado,
        String nombre,
        String foto
) {
}
