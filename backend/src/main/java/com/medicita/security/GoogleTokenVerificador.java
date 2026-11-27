package com.medicita.security;

import java.util.Optional;

/** Comprueba el token (ID token) que Google entrega al frontend al iniciar sesion. */
public interface GoogleTokenVerificador {

    /** Falso si no hay un ID de cliente de Google configurado (el boton se oculta en el frontend). */
    boolean estaConfigurado();

    /** ID de cliente de Google configurado (dato publico) o vacio si no hay uno. */
    String clientId();

    /** Vacio si el token no es de nuestra aplicacion, esta vencido o su firma no es valida. */
    Optional<GoogleIdentidad> verificar(String credential);
}
