package com.medicita.controller;

import com.medicita.dto.request.LoginRequest;
import com.medicita.dto.request.RegistroRequest;
import com.medicita.dto.response.AuthResponse;
import com.medicita.dto.response.UsuarioResponse;
import com.medicita.security.UsuarioAutenticado;
import com.medicita.service.AuthService;
import com.medicita.service.LogoutService;
import com.medicita.service.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LogoutService logoutService;
    private final RegistroService registroService;

    public AuthController(
            AuthService authService,
            LogoutService logoutService,
            RegistroService registroService) {
        this.authService = authService;
        this.logoutService = logoutService;
        this.registroService = registroService;
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> registrar(
            @Valid @RequestBody RegistroRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroService.registrar(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public MensajeResponse logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        String token = authorization.substring(7).trim();

        logoutService.cerrarSesion(token, usuario.getId());

        return new MensajeResponse("Sesion cerrada correctamente");
    }

    public record MensajeResponse(String message) {
    }
}