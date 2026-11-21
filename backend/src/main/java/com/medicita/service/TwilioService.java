package com.medicita.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Envio de SMS y WhatsApp con la API REST de Twilio (POST /Messages.json con autenticacion basica).
 * Esta apagado por defecto (app.twilio.enabled=false): en desarrollo solo escribe en el log.
 * Las claves viven en application.properties o en variables de entorno, nunca en el repositorio.
 * Un fallo al enviar se registra y no rompe la operacion que lo origino.
 *
 * Las cuentas de prueba de Twilio no aceptan texto libre:
 * - SMS: el cuerpo debe ser el nombre de una plantilla predefinida (app.twilio.sms-template).
 * - WhatsApp: solo se puede enviar el ContentSid de una plantilla de Twilio, sin variables (app.twilio.whatsapp-content-sid).
 * Si esas dos propiedades se dejan vacias (cuenta de pago) se envia el texto que arma la aplicacion.
 */
@Service
public class TwilioService {

    private static final Logger log = LoggerFactory.getLogger(TwilioService.class);
    // Formato internacional E.164: +52 y el numero, sin espacios
    private static final Pattern TELEFONO = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final boolean activo;
    private final String smsDesde;
    private final String smsPlantilla;
    private final String whatsappDesde;
    private final String whatsappContentSid;
    private final RestClient cliente;

    @Autowired
    public TwilioService(
            @Value("${app.twilio.enabled:false}") boolean activo,
            @Value("${app.twilio.account-sid:}") String accountSid,
            @Value("${app.twilio.auth-token:}") String authToken,
            @Value("${app.twilio.sms-from:}") String smsDesde,
            @Value("${app.twilio.sms-template:}") String smsPlantilla,
            @Value("${app.twilio.whatsapp-from:}") String whatsappDesde,
            @Value("${app.twilio.whatsapp-content-sid:}") String whatsappContentSid) {
        this(RestClient.builder(), activo, accountSid, authToken, smsDesde, smsPlantilla,
                whatsappDesde, whatsappContentSid);
    }

    TwilioService(RestClient.Builder constructor, boolean activo, String accountSid, String authToken,
                  String smsDesde, String smsPlantilla, String whatsappDesde, String whatsappContentSid) {
        this.activo = activo;
        this.smsDesde = smsDesde;
        this.smsPlantilla = smsPlantilla;
        this.whatsappDesde = whatsappDesde;
        this.whatsappContentSid = whatsappContentSid;
        this.cliente = constructor
                .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + accountSid)
                .defaultHeaders(cabeceras -> cabeceras.setBasicAuth(accountSid, authToken))
                .build();
    }

    /** Con una plantilla configurada (cuenta de prueba) no se puede enviar un texto propio. */
    public boolean smsAceptaTextoLibre() {
        return smsPlantilla.isBlank();
    }

    public void enviarSms(String telefono, String texto) {
        enviar(telefono, "SMS", campos -> {
            campos.add("To", telefono);
            campos.add("From", smsDesde);
            campos.add("Body", smsPlantilla.isBlank() ? texto : smsPlantilla);
        });
    }

    public void enviarWhatsApp(String telefono, String texto) {
        enviar(telefono, "WhatsApp", campos -> {
            campos.add("To", "whatsapp:" + telefono);
            campos.add("From", whatsappDesde);
            if (whatsappContentSid.isBlank()) {
                campos.add("Body", texto);
            } else {
                campos.add("ContentSid", whatsappContentSid);
            }
        });
    }

    private void enviar(String telefono, String canal, Consumer<LinkedMultiValueMap<String, String>> armar) {
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
            armar.accept(campos);
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
