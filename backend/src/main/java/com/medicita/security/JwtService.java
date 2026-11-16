package com.medicita.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private static final Set<String> ROLES_VALIDOS =
            Set.of("ADMIN", "MEDICO", "PACIENTE");

    private final SecretKey clave;
    private final Duration duracion;
    private final String emisor;
    private final JwtParser parser;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiration-minutes}") long minutos,
            @Value("${app.jwt.issuer}") String emisor) {

        if (minutos <= 0) {
            throw new IllegalArgumentException(
                    "La duracion del JWT debe ser mayor que cero");
        }

        if (emisor == null || emisor.isBlank()) {
            throw new IllegalArgumentException(
                    "El emisor del JWT no puede estar vacio");
        }

        this.clave = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secreto));
        this.duracion = Duration.ofMinutes(minutos);
        this.emisor = emisor;

        this.parser = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(emisor)
                .sig()
                    .clear()
                    .add(Jwts.SIG.HS256)
                    .and()
                .build();
    }

    public TokenGenerado generar(Long usuarioId, String rol) {

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del usuario es invalido");
        }

        if (rol == null || !ROLES_VALIDOS.contains(rol)) {
            throw new IllegalArgumentException("Rol invalido");
        }

        Instant ahora = Instant.now();
        Instant vencimiento = ahora.plus(duracion);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .issuer(emisor)
                .subject(usuarioId.toString())
                .claim("rol", rol)
                .id(jti)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(vencimiento))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();

        return new TokenGenerado(token, vencimiento);
    }

    public Claims validarYObtenerClaims(String token) {

        if (token == null || token.isBlank()) {
            throw new JwtException("Token ausente");
        }

        Claims claims = parser.parseSignedClaims(token).getPayload();

        if (claims.getExpiration() == null
                || claims.getIssuedAt() == null
                || claims.getId() == null
                || claims.getId().isBlank()) {
            throw new JwtException("Token incompleto");
        }

        try {
            if (Long.parseLong(claims.getSubject()) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new JwtException("Identificador de usuario invalido");
        }

        String rol = claims.get("rol", String.class);
        if (rol == null || !ROLES_VALIDOS.contains(rol)) {
            throw new JwtException("Rol del token invalido");
        }

        return claims;
    }

    public record TokenGenerado(String token, Instant expiraEn) {
    }
}