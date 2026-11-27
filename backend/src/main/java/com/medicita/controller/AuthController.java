package com.medicita.controller;

import com.medicita.dto.request.ForgotPasswordRequest;
import com.medicita.dto.request.GoogleLoginRequest;
import com.medicita.dto.request.LoginRequest;
import com.medicita.dto.request.RegistroRequest;
import com.medicita.dto.request.ResetPasswordRequest;
import com.medicita.dto.response.AuthResponse;
import com.medicita.dto.response.GoogleConfigResponse;
import com.medicita.dto.response.UsuarioResponse;
import com.medicita.security.UsuarioAutenticado;
import com.medicita.service.AuthService;
import com.medicita.service.GoogleAuthService;
import com.medicita.service.LogoutService;
import com.medicita.service.RecuperacionPasswordService;
import com.medicita.service.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final LogoutService logoutService;
    private final RegistroService registroService;
    private final RecuperacionPasswordService recuperacionPasswordService;

    public AuthController(
            AuthService authService,
            GoogleAuthService googleAuthService,
            LogoutService logoutService,
            RegistroService registroService,
            RecuperacionPasswordService recuperacionPasswordService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
        this.logoutService = logoutService;
        this.registroService = registroService;
        this.recuperacionPasswordService = recuperacionPasswordService;
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

    /** Publica: el frontend la consulta para saber si muestra el boton de Google y con que ID de cliente. */
    @GetMapping("/google/config")
    public GoogleConfigResponse googleConfig() {
        return googleAuthService.configuracion();
    }

    /** Inicio de sesion y registro con Google: recibe el token de Google y entrega el JWT de la aplicacion. */
    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return googleAuthService.iniciarSesion(request.credential());
    }

    @PostMapping("/logout")
    public MensajeResponse logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        String token = authorization.substring(7).trim();

        logoutService.cerrarSesion(token, usuario.getId());

        return new MensajeResponse("Sesion cerrada correctamente");
    }

    /** Responde lo mismo exista o no el correo: asi nadie averigua que correos estan registrados. */
    @PostMapping("/forgot-password")
    public MensajeResponse olvidePassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        recuperacionPasswordService.solicitar(request.email());
        return new MensajeResponse("Si el correo existe, recibiras un enlace");
    }

    @PostMapping("/reset-password")
    public MensajeResponse restablecerPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        recuperacionPasswordService.restablecer(request.token(), request.password());
        return new MensajeResponse("Contrasena actualizada. Ya puedes iniciar sesion");
    }

    public record MensajeResponse(String message) {
    }
}
