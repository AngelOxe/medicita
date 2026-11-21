package com.medicita.exception;

/**
 * Se incumplio una regla de negocio (horario traslapado, cita sin cobertura, enlace vencido...).
 * El GlobalExceptionHandler la convierte en 422. Si trae campo, el mensaje se muestra bajo ese input.
 */
public class ReglaNegocioException extends RuntimeException {

    private final String campo;

    public ReglaNegocioException(String mensaje) {
        this(null, mensaje);
    }

    public ReglaNegocioException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
