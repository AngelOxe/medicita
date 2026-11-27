package com.medicita.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** WhatsApp por la API de Meta: URL, token y plantilla con sus variables; nada se envia si esta apagado o el telefono es invalido. */
class WhatsAppCloudServiceTest {

    private static final String URL = "https://graph.facebook.com/v21.0/123456789/messages";

    private RestClient.Builder constructor;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
    }

    private WhatsAppCloudService whatsapp(boolean activo, String token, String plantillaCancelada) {
        return new WhatsAppCloudService(constructor, activo, "v21.0", "123456789", token,
                "cita_confirmada", plantillaCancelada, "es_MX");
    }

    @Test
    void laConfirmacionSeEnviaComoPlantillaConSusTresVariablesYElToken() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("525512345678"))
                .andExpect(jsonPath("$.type").value("template"))
                .andExpect(jsonPath("$.template.name").value("cita_confirmada"))
                .andExpect(jsonPath("$.template.language.code").value("es_MX"))
                .andExpect(jsonPath("$.template.components[0].type").value("body"))
                .andExpect(jsonPath("$.template.components[0].parameters[0].text").value("Ana"))
                .andExpect(jsonPath("$.template.components[0].parameters[1].text").value("Consulta general"))
                .andExpect(jsonPath("$.template.components[0].parameters[2].text").value("lunes 30 de noviembre de 2026, 10:00 h"))
                .andRespond(withSuccess());

        whatsapp(true, "token-de-prueba", "")
                .enviarConfirmacion("+525512345678", "Ana", "Consulta general", "lunes 30 de noviembre de 2026, 10:00 h");

        servidor.verify();
    }

    @Test
    void lasVariablesNoLlevanSaltosDeLineaNiEspaciosRepetidos() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.template.components[0].parameters[0].text").value("Ana Maria"))
                .andRespond(withSuccess());

        whatsapp(true, "t", "").enviarConfirmacion("+525512345678", "Ana\n   Maria", "Consulta", "hoy");

        servidor.verify();
    }

    @Test
    void laCancelacionSoloSeEnviaSiHayPlantillaConfigurada() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.template.name").value("cita_cancelada"))
                .andRespond(withSuccess());

        whatsapp(true, "t", "cita_cancelada").enviarCancelacion("+525512345678", "Ana", "Consulta", "hoy");
        servidor.verify();

        // Sin plantilla de cancelacion no se hace ninguna llamada (el servidor falso fallaria si llegara una)
        whatsapp(true, "t", "").enviarCancelacion("+525512345678", "Ana", "Consulta", "hoy");
    }

    @Test
    void apagadoNoEnviaNada() {
        whatsapp(false, "t", "").enviarConfirmacion("+525512345678", "Ana", "Consulta", "hoy");

        servidor.verify();
        assertFalse(whatsapp(false, "t", "").estaActivo());
    }

    @Test
    void sinTokenNoSeConsideraActivoAunqueEsteEncendido() {
        assertFalse(whatsapp(true, "", "").estaActivo());
        assertTrue(whatsapp(true, "t", "").estaActivo());
    }

    @Test
    void unTelefonoSinFormatoInternacionalNoSeEnvia() {
        whatsapp(true, "t", "").enviarConfirmacion("5512345678", "Ana", "Consulta", "hoy");
        whatsapp(true, "t", "").enviarConfirmacion(null, "Ana", "Consulta", "hoy");

        servidor.verify();
    }

    @Test
    void unFalloDeMetaSeRegistraYNoRompeLaOperacion() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertDoesNotThrow(() ->
                whatsapp(true, "t", "").enviarConfirmacion("+525512345678", "Ana", "Consulta", "hoy"));
    }
}
