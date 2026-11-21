package com.medicita.service;

import com.medicita.entity.Cita;
import com.medicita.entity.Servicio;
import com.medicita.entity.Usuario;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Envio de correo: se manda cuando esta activo, no se manda cuando esta apagado y un fallo no rompe nada. */
class NotificacionServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private JavaMailSender sender;
    private ObjectProvider<JavaMailSender> proveedor;
    private Cita cita;
    private Usuario paciente;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void preparar() {
        sender = mock(JavaMailSender.class);
        // createMimeMessage solo arma el mensaje en memoria; no abre ninguna conexion
        when(sender.createMimeMessage()).thenAnswer(i -> new JavaMailSenderImpl().createMimeMessage());
        proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(sender);

        paciente = new Usuario();
        paciente.setNombre("Ana López");
        paciente.setEmail("ana@example.com");
        Servicio servicio = new Servicio();
        servicio.setNombre("Consulta general");
        cita = new Cita();
        cita.setPaciente(paciente);
        cita.setServicio(servicio);
        cita.setInicio(OffsetDateTime.parse("2026-11-23T10:00:00-06:00"));
    }

    private NotificacionService servicio(boolean activo) {
        return new NotificacionService("https://sitio.test", activo, "no-reply@sitio.test", proveedor, ZONA);
    }

    @Test
    void conElCorreoActivoSeEnviaLaConfirmacionDeCita() throws Exception {
        servicio(true).notificarCitaConfirmada(cita);

        verify(sender).send(any(MimeMessage.class));
    }

    @Test
    void conElCorreoApagadoNoSeEnviaNada() {
        servicio(false).notificarCitaConfirmada(cita);
        servicio(false).notificarCitaCancelada(cita);
        servicio(false).enviarRecuperacionPassword(paciente, "token");

        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void siElEnvioFallaLaOperacionPrincipalNoSeRompe() {
        doThrow(new MailSendException("sin conexion")).when(sender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> servicio(true).notificarCitaCancelada(cita));
    }

    @Test
    void elCorreoDeRecuperacionLlevaElEnlaceConElToken() throws Exception {
        MimeMessage[] enviado = new MimeMessage[1];
        org.mockito.Mockito.doAnswer(i -> {
            enviado[0] = i.getArgument(0);
            return null;
        }).when(sender).send(any(MimeMessage.class));

        servicio(true).enviarRecuperacionPassword(paciente, "abc123");

        assertEquals("ana@example.com", enviado[0].getAllRecipients()[0].toString());
        assertTrue(enviado[0].getContent().toString().contains("https://sitio.test/restablecer?token=abc123"));
    }
}
