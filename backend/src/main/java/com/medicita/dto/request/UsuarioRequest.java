package com.medicita.dto.request;

import com.medicita.validation.MaxBytesUtf8;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** Cuerpo de POST /usuarios (solo ADMIN): crea una cuenta con el rol que se indique. */
public record UsuarioRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 254, message = "El correo admite hasta 254 caracteres")
        String email,

        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$",
                message = "Usa formato internacional: + y entre 8 y 15 digitos")
        String telefono,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @MaxBytesUtf8(max = 72)
        @Pattern(regexp = "(?s)^(?=.*\\p{Lu})(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$",
                message = "Incluye una mayuscula, un numero y un simbolo")
        String password,

        @NotNull(message = "El rol es obligatorio")
        @Pattern(regexp = "ADMIN|MEDICO|PACIENTE", message = "El rol debe ser ADMIN, MEDICO o PACIENTE")
        String rol
) {

    public UsuarioRequest {
        nombre = nombre == null ? null : nombre.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        telefono = telefono == null ? null : telefono.strip();
    }

    /** Cuerpo de PUT /usuarios/{id}: no cambia correo, rol ni contrasena. */
    public record Actualizar(

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
            String nombre,

            @NotBlank(message = "El telefono es obligatorio")
            @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$",
                    message = "Usa formato internacional: + y entre 8 y 15 digitos")
            String telefono,

            @NotNull(message = "Indica si la cuenta esta activa")
            Boolean activo
    ) {

        public Actualizar {
            nombre = nombre == null ? null : nombre.strip();
            telefono = telefono == null ? null : telefono.strip();
        }
    }

    /** Cuerpo de PUT /perfil: la persona edita solo su nombre y telefono. */
    public record Perfil(

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
            String nombre,

            @NotBlank(message = "El telefono es obligatorio")
            @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$",
                    message = "Usa formato internacional: + y entre 8 y 15 digitos")
            String telefono
    ) {

        public Perfil {
            nombre = nombre == null ? null : nombre.strip();
            telefono = telefono == null ? null : telefono.strip();
        }
    }
}
