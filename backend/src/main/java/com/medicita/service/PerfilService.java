package com.medicita.service;

import com.medicita.dto.request.UsuarioRequest;
import com.medicita.dto.response.UsuarioResponse;
import com.medicita.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;

    public PerfilService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfil(Long usuarioId) {

        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new BadCredentialsException("Usuario no disponible")
                );

        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Usuario no disponible");
        }

        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public UsuarioResponse actualizar(Long usuarioId, UsuarioRequest.Perfil request) {

        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new BadCredentialsException("Usuario no disponible")
                );

        usuario.setNombre(request.nombre());
        usuario.setTelefono(request.telefono());
        return UsuarioResponse.de(usuario);
    }
}