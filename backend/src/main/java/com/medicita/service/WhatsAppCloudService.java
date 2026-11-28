package com.medicita.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Envio de WhatsApp con la API oficial de Meta (WhatsApp Cloud API).
 * Los mensajes que inicia el negocio SOLO se pueden enviar con una PLANTILLA aprobada por Meta, con
 * variables ({{1}}, {{2}}, {{3}}). Aqui la plantilla es nuestra: el texto se define en el panel de Meta
 * y el backend solo manda el nombre de la plantilla y los valores de las variables.
 * Esta apagado por defecto (app.whatsapp.enabled=false): en desarrollo solo escribe en el log.
 * El token y el ID del numero viven en application.properties o en variables de entorno, nunca en el repositorio.
 * Un fallo al enviar se registra y no rompe la operacion que lo origino.
 */
@Service
public class WhatsAppCloudService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudService.class);
    // Formato internacional E.164: +52 y el numero, sin espacios
    private static final Pattern TELEFONO = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final boolean activo;
    private final boolean configurado;
    private final String phoneNumberId;
    private final String plantillaConfirmada;
    private final String plantillaCancelada;
    private final String plantillaBienvenida;
    private final String idioma;
    private final RestClient cliente;

    @Autowired
    public WhatsAppCloudService(
            @Value("${app.whatsapp.enabled:false}") boolean activo,
            @Value("${app.whatsapp.api-version:v21.0}") String version,
            @Value("${app.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${app.whatsapp.token:}") String token,
            @Value("${app.whatsapp.template-confirmada:cita_confirmada}") String plantillaConfirmada,
            @Value("${app.whatsapp.template-cancelada:}") String plantillaCancelada,
            @Value("${app.whatsapp.template-bienvenida:}") String plantillaBienvenida,
            @Value("${app.whatsapp.language:es_MX}") String idioma) {
        this(RestClient.builder(), activo, version, phoneNumberId, token, plantillaConfirmada, plantillaCancelada,
                plantillaBienvenida, idioma);
    }

    WhatsAppCloudService(RestClient.Builder constructor, boolean activo, String version, String phoneNumberId,
                         String token, String plantillaConfirmada, String plantillaCancelada,
                         String plantillaBienvenida, String idioma) {
        this.activo = activo;
        this.phoneNumberId = phoneNumberId == null ? "" : phoneNumberId.trim();
        this.configurado = !this.phoneNumberId.isBlank() && token != null && !token.isBlank();
        this.plantillaConfirmada = plantillaConfirmada == null ? "" : plantillaConfirmada.trim();
        this.plantillaCancelada = plantillaCancelada == null ? "" : plantillaCancelada.trim();
        this.plantillaBienvenida = plantillaBienvenida == null ? "" : plantillaBienvenida.trim();
        this.idioma = idioma;
        this.cliente = constructor
                .baseUrl("https://graph.facebook.com/" + version)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + (token == null ? "" : token.trim()))
                .build();
    }

    /** Verdadero si el WhatsApp de Meta esta encendido y tiene su ID de numero y su token. */
    public boolean estaActivo() {
        return activo && configurado;
    }

    /** Variables de la plantilla de confirmacion: {{1}} paciente, {{2}} servicio, {{3}} fecha y hora. */
    public boolean enviarConfirmacion(String telefono, String paciente, String servicio, String fechaHora) {
        return enviarPlantilla(telefono, plantillaConfirmada, List.of(paciente, servicio, fechaHora))
                || enviarTexto(telefono, "Hola " + paciente + ", tu cita de " + servicio + " quedó confirmada para el "
                        + fechaHora + ". MediCita");
    }

    /** Solo se envia si hay una plantilla de cancelacion configurada (app.whatsapp.template-cancelada). */
    public boolean enviarCancelacion(String telefono, String paciente, String servicio, String fechaHora) {
        return enviarPlantilla(telefono, plantillaCancelada, List.of(paciente, servicio, fechaHora))
                || enviarTexto(telefono, "Hola " + paciente + ", tu cita de " + servicio + " del " + fechaHora
                        + " fue cancelada. Puedes reservar otra cuando quieras en MediCita.");
    }

    /** Plantilla de bienvenida al crear una cuenta, con UNA variable: {{1}} nombre. */
    public boolean enviarBienvenida(String telefono, String nombre) {
        return enviarPlantilla(telefono, plantillaBienvenida, List.of(nombre))
                || enviarTexto(telefono, "Hola " + nombre + ", tu cuenta en MediCita se creó correctamente. "
                        + "Ya puedes iniciar sesión y reservar tus citas médicas.");
    }

    /**
     * Respaldo sin plantilla: Meta solo acepta texto libre dentro de la ventana de 24 horas que se abre cuando
     * el usuario escribe primero al numero del negocio. Fuera de esa ventana Meta lo rechaza y solo se registra.
     */
    private boolean enviarTexto(String telefono, String texto) {
        if (telefono == null || !TELEFONO.matcher(telefono).matches() || !estaActivo()) {
            return false;
        }
        try {
            Map<String, Object> cuerpo = Map.of(
                    "messaging_product", "whatsapp",
                    "to", destinoMeta(telefono),
                    "type", "text",
                    "text", Map.of("body", texto));
            cliente.post()
                    .uri("/{id}/messages", phoneNumberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[WhatsApp Meta] Texto libre enviado a {} (ventana de 24 h abierta)", telefono);
            return true;
        } catch (Exception error) {
            log.warn("[WhatsApp Meta] Texto libre no enviado a {} (el usuario debe escribir primero): {}",
                    telefono, error.getMessage());
            return false;
        }
    }

    /** Devuelve true solo si Meta acepto el mensaje; false si no se envio o fallo (el llamador puede usar un respaldo). */
    private boolean enviarPlantilla(String telefono, String plantilla, List<String> variables) {
        if (plantilla.isBlank()) {
            return false;
        }
        if (telefono == null || !TELEFONO.matcher(telefono).matches()) {
            log.info("[WhatsApp] No se envia: el paciente no tiene un telefono en formato internacional");
            return false;
        }
        if (!estaActivo()) {
            log.info("[WhatsApp Meta simulado] Plantilla {} para {}", plantilla, telefono);
            return false;
        }
        try {
            List<Map<String, String>> parametros = new ArrayList<>();
            for (String valor : variables) {
                parametros.add(Map.of("type", "text", "text", limpiar(valor)));
            }
            Map<String, Object> cuerpo = Map.of(
                    "messaging_product", "whatsapp",
                    "to", destinoMeta(telefono),
                    "type", "template",
                    "template", Map.of(
                            "name", plantilla,
                            "language", Map.of("code", idioma),
                            "components", List.of(Map.of("type", "body", "parameters", parametros))));

            cliente.post()
                    .uri("/{id}/messages", phoneNumberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[WhatsApp Meta] Plantilla {} enviada a {}", plantilla, telefono);
            return true;
        } catch (Exception error) {
            // La cita ya se guardo o se cancelo: solo se deja constancia del fallo
            log.warn("[WhatsApp Meta] No se pudo enviar a {}: {}", telefono, error.getMessage());
            return false;
        }
    }

    /**
     * Meta entrega los celulares de Mexico con el formato 521 + 10 digitos (con un 1 despues de la lada).
     * Con +52 y 10 digitos el mensaje se acepta pero no se entrega, asi que el 1 se agrega aqui.
     */
    static String destinoMeta(String telefono) {
        String digitos = telefono.substring(1);
        if (digitos.length() == 12 && digitos.startsWith("52")) {
            return "521" + digitos.substring(2);
        }
        return digitos;
    }

    // Meta no acepta saltos de linea, tabuladores ni cuatro espacios seguidos en una variable
    private String limpiar(String valor) {
        String texto = valor == null || valor.isBlank() ? "-" : valor;
        return texto.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" {2,}", " ").trim();
    }
}
