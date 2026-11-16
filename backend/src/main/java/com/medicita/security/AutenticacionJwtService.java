package com.medicita.security;

import com.medicita.repository.TokenRevocadoRepository;
import com.medicita.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacionJwtService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRevocadoRepository tokenRevocadoRepository;

    public AutenticacionJwtService(
            UsuarioRepository usuarioRepository,
            TokenRevocadoRepository tokenRevocadoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRevocadoRepository = tokenRevocadoRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticado cargarUsuario(Long usuarioId, String jti) {

        if (tokenRevocadoRepository.existsById(jti)) {
            throw new BadCredentialsException("Token revocado");
        }

        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new BadCredentialsException("Usuario no disponible")
                );

        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Usuario no disponible");
        }

        return new UsuarioAutenticado(usuario);
    }
}