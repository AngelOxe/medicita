package com.medicita.dto.request;

import com.medicita.validation.MaxBytesUtf8;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Misma regla de contrasena que el registro: se valida aqui con Bean Validation. */
public record ResetPasswordRequest(

        @NotBlank(message = "El token es obligatorio")
        @Size(max = 200, message = "El token no es valido")
        String token,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(
                min = 8,
                max = 72,
                message = "La contrasena debe tener entre 8 y 72 caracteres"
        )
        @MaxBytesUtf8(max = 72)
        @Pattern(
                regexp = "(?s)^(?=.*\\p{Lu})(?=.*[0-9])"
                        + "(?=.*[^\\p{L}\\p{N}\\s]).*$",
                message = "Incluye una mayuscula, un numero y un simbolo"
        )
        String password
) {

    public ResetPasswordRequest {
        token = token == null ? null : token.strip();
    }
}
