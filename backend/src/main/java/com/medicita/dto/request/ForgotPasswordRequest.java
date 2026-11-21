package com.medicita.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record ForgotPasswordRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 254, message = "El correo admite hasta 254 caracteres")
        String email
) {

    public ForgotPasswordRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
