package com.medicita.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Envio por la API de Twilio: URL, autenticacion y campos correctos; nada se envia si esta apagado o el telefono es invalido. */
class TwilioServiceTest {

    private static final String URL = "https://api.twilio.com/2010-04-01/Accounts/ACprueba/Messages.json";

    private RestClient.Builder constructor;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
    }

    private TwilioService twilio(boolean activo) {
        return new TwilioService(constructor, activo, "ACprueba", "token",
                "+15005550006", "", "whatsapp:+14155238886", "");
    }

    /** Cuenta de prueba: SMS con plantilla y WhatsApp con ContentSid. */
    private TwilioService twilioDePrueba() {
        return new TwilioService(constructor, true, "ACprueba", "token",
                "+15005550006", "sms_appointment_reminders", "whatsapp:+14155238886", "HX123");
    }

    @Test
    void elSmsSeEnviaConAutenticacionBasicaYLosCamposDeTwilio() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", startsWith("Basic ")))
                .andExpect(content().formDataContains(Map.of("To", "+525512345678", "From", "+15005550006")))
                .andRespond(withSuccess());

        twilio(true).enviarSms("+525512345678", "Hola");

        servidor.verify();
    }

    @Test
    void elWhatsAppUsaElPrefijoWhatsappEnOrigenYDestino() {
        servidor.expect(requestTo(URL))
                .andExpect(content().formDataContains(
                        Map.of("To", "whatsapp:+525512345678", "From", "whatsapp:+14155238886")))
                .andRespond(withSuccess());

        twilio(true).enviarWhatsApp("+525512345678", "Hola", "23/11/2026", "10:00");

        servidor.verify();
    }

    @Test
    void conCuentaDePruebaElSmsEnviaElNombreDeLaPlantillaComoCuerpo() {
        servidor.expect(requestTo(URL))
                .andExpect(content().formDataContains(Map.of("Body", "sms_appointment_reminders")))
                .andRespond(withSuccess());

        twilioDePrueba().enviarSms("+525512345678", "texto que no se usa");

        servidor.verify();
    }

    @Test
    void conCuentaDePruebaElWhatsAppEnviaContentSidYVariablesDeFechaYHora() {
        servidor.expect(requestTo(URL))
                .andExpect(content().formDataContains(Map.of(
                        "ContentSid", "HX123",
                        "ContentVariables", "{\"1\":\"23/11/2026\",\"2\":\"10:00\"}")))
                .andRespond(withSuccess());

        twilioDePrueba().enviarWhatsApp("+525512345678", "texto", "23/11/2026", "10:00");

        servidor.verify();
    }

    @Test
    void conTwilioApagadoOConTelefonoInvalidoNoSeLlamaALaApi() {
        twilio(false).enviarSms("+525512345678", "Hola");
        twilio(true).enviarSms("0000000000", "Hola");
        twilio(true).enviarWhatsApp(null, "Hola", "x", "y");

        servidor.verify(); // no habia ninguna peticion esperada
    }

    @Test
    void siTwilioRespondeConErrorLaOperacionNoSeRompe() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertDoesNotThrow(() -> twilio(true).enviarSms("+525512345678", "Hola"));
    }
}
