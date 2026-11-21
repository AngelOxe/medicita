package com.medicita.service;

import com.medicita.entity.PasswordResetToken;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.entity.Usuario;
import com.medicita.repository.PasswordResetTokenRepository;
import com.medicita.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperacion de contrasena.
 * El token real solo viaja por correo; en la base de datos se guarda su hash SHA-256,
 * asi que quien lea la tabla no puede usarlo. Vence en 30 minutos y sirve una sola vez.
 */
@Service
public class RecuperacionPasswordService {

    private static final long MINUTOS_VIGENCIA = 30;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificacionService;
    private final SecureRandom random = new SecureRandom();

    public RecuperacionPasswordService(
            UsuarioRepository usuarioRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificacionService = notificacionService;
    }

    /** No revela si el correo existe: si no existe o esta inactivo, simplemente no hace nada. */
    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(Usuario::isActivo)
                .ifPresent(usuario -> {
                    // Un enlace nuevo invalida los anteriores
                    tokenRepository.invalidarPendientes(usuario.getId());

                    String token = generarToken();
                    tokenRepository.save(new PasswordResetToken(
                            usuario,
                            sha256(token),
                            OffsetDateTime.now().plusMinutes(MINUTOS_VIGENCIA)
                    ));
                    notificacionService.enviarRecuperacionPassword(usuario, token);
                });
    }

    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        PasswordResetToken registro = tokenRepository.findByTokenHash(sha256(token))
                .filter(t -> !t.isUsado()
                        && t.getExpiraEn().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new ReglaNegocioException(
                        "token",
                        "El enlace no es valido o ya vencio. Solicita uno nuevo"));

        registro.getUsuario().setPasswordHash(passwordEncoder.encode(nuevaPassword));
        registro.setUsado(true);
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
