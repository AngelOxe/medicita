package com.medicita.service;

import com.medicita.dto.request.LoginRequest;
import com.medicita.dto.response.AuthResponse;
import com.medicita.dto.response.UsuarioResponse;
import com.medicita.repository.UsuarioRepository;
import com.medicita.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UsuarioRepository usuarioRepository,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        var autenticacion = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.email().trim(),
                        request.password()
                )
        );

        var usuario = usuarioRepository
                .findByEmailIgnoreCase(autenticacion.getName())
                .orElseThrow(() ->
                        new BadCredentialsException("Credenciales incorrectas")
                );

        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        var token = jwtService.generar(
                usuario.getId(),
                usuario.getRol().getNombre()
        );

        return new AuthResponse(
                token.token(),
                "Bearer",
                token.expiraEn(),
                UsuarioResponse.de(usuario)
        );
    }
}