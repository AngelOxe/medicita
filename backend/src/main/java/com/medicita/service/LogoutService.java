package com.medicita.service;

import com.medicita.entity.TokenRevocado;
import com.medicita.repository.TokenRevocadoRepository;
import com.medicita.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class LogoutService {

    private final JwtService jwtService;
    private final TokenRevocadoRepository tokenRevocadoRepository;

    public LogoutService(
            JwtService jwtService,
            TokenRevocadoRepository tokenRevocadoRepository) {
        this.jwtService = jwtService;
        this.tokenRevocadoRepository = tokenRevocadoRepository;
    }

    @Transactional
    public void cerrarSesion(String token, Long usuarioId) {

        var claims = jwtService.validarYObtenerClaims(token);

        if (!usuarioId.toString().equals(claims.getSubject())) {
            throw new BadCredentialsException("Token no valido");
        }

        String jti = claims.getId();

        OffsetDateTime expiraEn = OffsetDateTime.ofInstant(
                claims.getExpiration().toInstant(),
                ZoneOffset.UTC
        );

        if (!tokenRevocadoRepository.existsById(jti)) {
            tokenRevocadoRepository.save(
                    new TokenRevocado(jti, expiraEn)
            );
        }
    }
}