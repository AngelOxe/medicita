package com.medicita.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

/**
 * Verifica el ID token con la libreria oficial de Google: comprueba la firma con las llaves
 * publicas de Google, que el emisor sea Google, que no haya vencido y que este dirigido a
 * NUESTRO ID de cliente (app.google.client-id). El ID de cliente es publico, no es un secreto.
 */
@Component
public class GoogleTokenVerificadorImpl implements GoogleTokenVerificador {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerificadorImpl.class);

    private final GoogleIdTokenVerifier verificador;
    private final String clientId;

    public GoogleTokenVerificadorImpl(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        if (clientId == null || clientId.isBlank()) {
            this.verificador = null;
        } else {
            this.verificador = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId.trim()))
                    .build();
        }
    }

    @Override
    public boolean estaConfigurado() {
        return verificador != null;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public Optional<GoogleIdentidad> verificar(String credential) {
        if (verificador == null || credential == null || credential.isBlank()) {
            return Optional.empty();
        }
        try {
            GoogleIdToken token = verificador.verify(credential);
            if (token == null) {
                return Optional.empty();
            }
            var datos = token.getPayload();
            return Optional.of(new GoogleIdentidad(
                    datos.getSubject(),
                    datos.getEmail(),
                    Boolean.TRUE.equals(datos.getEmailVerified()),
                    (String) datos.get("name"),
                    (String) datos.get("picture")));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException error) {
            log.warn("No se pudo verificar el token de Google: {}", error.getMessage());
            return Optional.empty();
        }
    }
}
