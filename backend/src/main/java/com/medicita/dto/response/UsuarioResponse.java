package com.medicita.dto.response;

import com.medicita.entity.Usuario;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String avatarUrl,
        String rol,
        boolean activo
) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getAvatarUrl(),
                usuario.getRol().getNombre(),
                usuario.isActivo()
        );
    }
}