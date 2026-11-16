package com.medicita.security;

import com.medicita.exception.ApiError;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AutenticacionJwtService autenticacionJwtService;
    private final JsonMapper jsonMapper;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AutenticacionJwtService autenticacionJwtService,
            JsonMapper jsonMapper) {
        this.jwtService = jwtService;
        this.autenticacionJwtService = autenticacionJwtService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (encabezado == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!encabezado.regionMatches(true, 0, "Bearer ", 0, 7)) {
                throw new BadCredentialsException(
                        "Formato de autorizacion incorrecto"
                );
            }

            String token = encabezado.substring(7).trim();

            if (token.isEmpty()) {
                throw new BadCredentialsException("Token vacio");
            }

            var claims = jwtService.validarYObtenerClaims(token);

            Long usuarioId = Long.valueOf(claims.getSubject());

            var usuario = autenticacionJwtService.cargarUsuario(
                    usuarioId,
                    claims.getId()
            );

            var autenticacion =
                    UsernamePasswordAuthenticationToken.authenticated(
                            usuario,
                            null,
                            usuario.getAuthorities()
                    );

            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacion);
            SecurityContextHolder.setContext(contexto);

        } catch (JwtException | AuthenticationException
                 | IllegalArgumentException exception) {

            SecurityContextHolder.clearContext();
            responderNoAutorizado(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void responderNoAutorizado(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        var error = new ApiError(
                Instant.now(),
                401,
                "Token invalido, vencido o usuario no disponible",
                request.getRequestURI(),
                List.of()
        );

        response.getWriter().write(jsonMapper.writeValueAsString(error));
    }
}