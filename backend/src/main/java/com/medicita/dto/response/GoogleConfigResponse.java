package com.medicita.dto.response;

/** Le dice al frontend si debe mostrar el boton de Google y con que ID de cliente (dato publico). */
public record GoogleConfigResponse(boolean habilitado, String clientId) {
}
