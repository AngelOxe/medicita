package com.medicita.service;

import com.medicita.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Punto unico de comunicacion con el usuario.
 * Por ahora los mensajes solo se escriben en el log; cuando Postfix y Twilio esten listos,
 * aqui se enviaran de verdad (correo con JavaMailSender, SMS y WhatsApp con Twilio)
 * sin tocar el resto del codigo.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final String frontendUrl;

    public NotificacionService(
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public void enviarRecuperacionPassword(Usuario usuario, String token) {
        String enlace = frontendUrl + "/restablecer?token=" + token;
        log.info("[CORREO simulado] Recuperacion de contrasena para {}: {}",
                usuario.getEmail(), enlace);
    }
}
