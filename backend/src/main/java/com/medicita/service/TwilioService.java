package com.medicita.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.regex.Pattern;

/**
 * Envio de SMS y WhatsApp con la API REST de Twilio (POST /Messages.json con autenticacion basica).
 * Esta apagado por defecto (app.twilio.enabled=false): en desarrollo solo escribe en el log.
 * Las claves viven en application.properties o en variables de entorno, nunca en el repositorio.
 * Un fallo al enviar se registra y no rompe la operacion que lo origino.
 */
@Service
public class TwilioService {

    private static final Logger log = LoggerFactory.getLogger(TwilioService.class);
    // Formato internacional E.164: +52 y el numero, sin espacios
    private static final Pattern TELEFONO = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final boolean activo;
    private final String smsDesde;
    private final String whatsappDesde;
    private final RestClient cliente;

    @Autowired
    public TwilioService(
            @Value("${app.twilio.enabled:false}") boolean activo,
            @Value("${app.twilio.account-sid:}") String accountSid,
            @Value("${app.twilio.auth-token:}") String authToken,
            @Value("${app.twilio.sms-from:}") String smsDesde,
            @Value("${app.twilio.whatsapp-from:}") String whatsappDesde) {
        this(RestClient.builder(), activo, accountSid, authToken, smsDesde, whatsappDesde);
    }

    TwilioService(RestClient.Builder constructor, boolean activo, String accountSid, String authToken,
                  String smsDesde, String whatsappDesde) {
        this.activo = activo;
        this.smsDesde = smsDesde;
        this.whatsappDesde = whatsappDesde;
        this.cliente = constructor
                .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + accountSid)
                .defaultHeaders(cabeceras -> cabeceras.setBasicAuth(accountSid, authToken))
                .build();
    }

    public void enviarSms(String telefono, String texto) {
        enviar(telefono, smsDesde, telefono, texto, "SMS");
    }

    public void enviarWhatsApp(String telefono, String texto) {
        enviar("whatsapp:" + telefono, whatsappDesde, telefono, texto, "WhatsApp");
    }

    private void enviar(String para, String desde, String telefono, String texto, String canal) {
        if (telefono == null || !TELEFONO.matcher(telefono).matches()) {
            log.info("[{}] No se envia: el paciente no tiene un telefono en formato internacional", canal);
            return;
        }
        if (!activo) {
            log.info("[{} simulado] Mensaje para {}", canal, telefono);
            return;
        }
        try {
            var campos = new LinkedMultiValueMap<String, String>();
            campos.add("To", para);
            campos.add("From", desde);
            campos.add("Body", texto);
            cliente.post()
                    .uri("/Messages.json")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(campos)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[{}] Mensaje enviado a {}", canal, telefono);
        } catch (Exception error) {
            // La cita ya se guardo o se cancelo: solo se deja constancia del fallo
            log.warn("[{}] No se pudo enviar a {}: {}", canal, telefono, error.getMessage());
        }
    }
}
