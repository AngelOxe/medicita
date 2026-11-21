package com.medicita.service;

import com.medicita.entity.Cita;
import com.medicita.entity.Usuario;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Punto unico de comunicacion con el usuario.
 * El correo sale por JavaMailSender hacia el Postfix local del servidor (que lo firma con DKIM).
 * Si app.mail.enabled=false (valor por defecto, util en desarrollo) los mensajes solo se escriben en el log.
 * Un fallo al enviar nunca rompe la operacion principal: se registra y la reserva o la cancelacion siguen.
 * El SMS y el WhatsApp salen por TwilioService: confirmar una cita envia WhatsApp y SMS.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm 'h'", Locale.forLanguageTag("es-MX"));

    private final String frontendUrl;
    private final boolean correoActivo;
    private final String remitente;
    private final ObjectProvider<JavaMailSender> correo;
    private final ZoneId zona;
    private final TwilioService twilio;

    public NotificacionService(
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${app.mail.enabled:false}") boolean correoActivo,
            @Value("${app.mail.from:no-reply@localhost}") String remitente,
            ObjectProvider<JavaMailSender> correo,
            ZoneId zona,
            TwilioService twilio) {
        this.frontendUrl = frontendUrl;
        this.correoActivo = correoActivo;
        this.remitente = remitente;
        this.correo = correo;
        this.zona = zona;
        this.twilio = twilio;
    }

    /** Reserva: correo de confirmacion (el WhatsApp se agrega con Twilio). */
    public void notificarCitaConfirmada(Cita cita) {
        Usuario paciente = cita.getPaciente();
        String cuerpo = "Hola " + paciente.getNombre() + ":\n\n"
                + "Tu cita quedó confirmada.\n\n"
                + detalle(cita)
                + "\nPuedes cancelarla hasta 2 horas antes desde \"Mis citas\":\n"
                + frontendUrl + "/mis-citas\n\n"
                + "MediCita";
        enviarCorreo(paciente.getEmail(), "Tu cita en MediCita está confirmada", cuerpo);
        String texto = "MediCita: tu cita de " + cita.getServicio().getNombre()
                + " quedó confirmada para el " + fecha(cita) + ".";
        twilio.enviarWhatsApp(paciente.getTelefono(), texto);
        twilio.enviarSms(paciente.getTelefono(), texto);
    }

    /** Cancelacion: correo, y SMS solo si la cuenta de Twilio acepta texto libre (no con plantillas de prueba). */
    public void notificarCitaCancelada(Cita cita) {
        Usuario paciente = cita.getPaciente();
        String cuerpo = "Hola " + paciente.getNombre() + ":\n\n"
                + "Tu cita fue cancelada.\n\n"
                + detalle(cita)
                + "\nSi lo necesitas, puedes reservar otra en:\n"
                + frontendUrl + "/reservar\n\n"
                + "MediCita";
        enviarCorreo(paciente.getEmail(), "Tu cita en MediCita fue cancelada", cuerpo);
        if (twilio.smsAceptaTextoLibre()) {
            twilio.enviarSms(paciente.getTelefono(), "MediCita: tu cita de " + cita.getServicio().getNombre()
                    + " del " + fecha(cita) + " fue cancelada.");
        }
    }

    public void enviarRecuperacionPassword(Usuario usuario, String token) {
        String enlace = frontendUrl + "/restablecer?token=" + token;
        String cuerpo = "Hola " + usuario.getNombre() + ":\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña. Abre este enlace "
                + "(vence en 30 minutos y solo sirve una vez):\n\n"
                + enlace + "\n\n"
                + "Si no fuiste tú, ignora este mensaje: tu contraseña no cambiará.\n\n"
                + "MediCita";
        // Con el correo apagado el enlace queda en el log para poder probar; con el correo activo nunca se escribe ahi
        if (!correoActivo) {
            log.info("[CORREO simulado] Recuperacion de contrasena para {}: {}", usuario.getEmail(), enlace);
            return;
        }
        enviarCorreo(usuario.getEmail(), "Restablece tu contraseña de MediCita", cuerpo);
    }

    private String fecha(Cita cita) {
        return FECHA_HORA.format(cita.getInicio().atZoneSameInstant(zona));
    }

    private String detalle(Cita cita) {
        return "Servicio: " + cita.getServicio().getNombre() + "\n"
                + "Fecha y hora: " + FECHA_HORA.format(cita.getInicio().atZoneSameInstant(zona)) + "\n";
    }

    private void enviarCorreo(String destino, String asunto, String texto) {
        JavaMailSender sender = correoActivo ? correo.getIfAvailable() : null;
        if (sender == null) {
            log.info("[CORREO simulado] Para {}: {}", destino, asunto);
            return;
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente, "MediCita");
            helper.setTo(destino);
            helper.setSubject(asunto);
            helper.setText(texto, false);
            sender.send(mensaje);
            log.info("Correo enviado a {}: {}", destino, asunto);
        } catch (Exception error) {
            // La reserva o la cancelacion ya se guardaron: solo se deja constancia del fallo
            log.warn("No se pudo enviar el correo a {}: {}", destino, error.getMessage());
        }
    }
}
