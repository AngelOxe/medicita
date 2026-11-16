package com.medicita.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private SecretKey clave;
    private JwtService jwtService;

    @BeforeEach
    void preparar() {
        clave = Jwts.SIG.HS256.key().build();

        String secreto = Base64.getEncoder()
                .encodeToString(clave.getEncoded());

        jwtService = new JwtService(secreto, 60, "medicita");
    }

    @Test
    void generaYValidaUnToken() {
        var generado = jwtService.generar(1L, "PACIENTE");
        var claims = jwtService.validarYObtenerClaims(generado.token());

        assertEquals("1", claims.getSubject());
        assertEquals("PACIENTE", claims.get("rol", String.class));
        assertEquals("medicita", claims.getIssuer());
        assertNotNull(claims.getId());
        assertFalse(claims.getId().isBlank());
        assertTrue(claims.getExpiration().toInstant().isAfter(Instant.now()));

        var segundo = jwtService.generar(1L, "PACIENTE");
        var otrosClaims =
                jwtService.validarYObtenerClaims(segundo.token());

        assertNotEquals(claims.getId(), otrosClaims.getId());
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        SecretKey otraClave = Jwts.SIG.HS256.key().build();
        Instant ahora = Instant.now();

        String token = Jwts.builder()
                .issuer("medicita")
                .subject("1")
                .claim("rol", "ADMIN")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(3600)))
                .signWith(otraClave, Jwts.SIG.HS256)
                .compact();

        assertThrows(
                JwtException.class,
                () -> jwtService.validarYObtenerClaims(token)
        );
    }

    @Test
    void rechazaUnTokenVencido() {
        Instant ahora = Instant.now();

        String token = Jwts.builder()
                .issuer("medicita")
                .subject("1")
                .claim("rol", "PACIENTE")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(ahora.minusSeconds(120)))
                .expiration(Date.from(ahora.minusSeconds(60)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.validarYObtenerClaims(token)
        );
    }
}