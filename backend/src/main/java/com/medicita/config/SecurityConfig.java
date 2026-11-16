package com.medicita.config;

import com.medicita.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;
import com.medicita.security.AutenticacionJwtService;
import com.medicita.security.JwtAuthenticationFilter;
import com.medicita.security.JwtService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JsonMapper jsonMapper,
        JwtService jwtService,
        AutenticacionJwtService autenticacionJwtService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/salud")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/auth/login")
                        .permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/perfil")
                        .authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/auth/logout")
                        .authenticated()

                        .anyRequest()
                        .denyAll()
                )

                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                escribirError(
                                        request,
                                        response,
                                        jsonMapper,
                                        401,
                                        "Se requiere autenticacion"
                                )
                        )

                        .accessDeniedHandler((request, response, exception) ->
                                escribirError(
                                        request,
                                        response,
                                        jsonMapper,
                                        403,
                                        "No tienes permiso para realizar esta operacion"
                                )
                        )
                );
                http.addFilterBefore(
        new JwtAuthenticationFilter(
                jwtService,
                autenticacionJwtService,
                jsonMapper
        ),
        UsernamePasswordAuthenticationFilter.class
);
        return http.build();
    }

    private void escribirError(
            HttpServletRequest request,
            HttpServletResponse response,
            JsonMapper jsonMapper,
            int status,
            String mensaje) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        var error = new ApiError(
                Instant.now(),
                status,
                mensaje,
                request.getRequestURI(),
                List.of()
        );

        response.getWriter().write(jsonMapper.writeValueAsString(error));
    }
}