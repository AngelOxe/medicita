package com.medicita.controller;

import com.medicita.dto.request.LoginRequest;
import com.medicita.dto.response.AuthResponse;
import com.medicita.security.UsuarioAutenticado;
import com.medicita.service.AuthService;
import com.medicita.service.LogoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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

    public AuthController(
            AuthService authService,
            LogoutService logoutService) {
        this.authService = authService;
        this.logoutService = logoutService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public MensajeResponse logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        // El filtro JWT ya comprobo el encabezado y autentico al usuario.
        String token = authorization.substring(7).trim();

        logoutService.cerrarSesion(token, usuario.getId());

        return new MensajeResponse("Sesion cerrada correctamente");
    }

    public record MensajeResponse(String message) {
    }
}