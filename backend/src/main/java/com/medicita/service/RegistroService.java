package com.medicita.service;

import com.medicita.dto.request.RegistroRequest;
import com.medicita.dto.response.UsuarioResponse;
import com.medicita.entity.Usuario;
import com.medicita.exception.CorreoDuplicadoException;
import com.medicita.repository.RolRepository;
import com.medicita.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

@Service
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificacionService;

    public RegistroService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {

        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new CorreoDuplicadoException();
        }

        var rolPaciente = rolRepository.findByNombre("PACIENTE")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el rol PACIENTE en la base de datos"
                        )
                );

        var usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setTelefono(request.telefono());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(rolPaciente);
        usuario.setActivo(true);

        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            if (esViolacionDeUnicidad(exception)) {
                throw new CorreoDuplicadoException();
            }
            throw exception;
        }

        notificacionService.notificarCuentaCreada(usuario);

        return UsuarioResponse.de(usuario);
    }

    private boolean esViolacionDeUnicidad(Throwable exception) {
        Throwable causa = exception;

        while (causa != null) {
            if (causa instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            causa = causa.getCause();
        }

        return false;
    }
}