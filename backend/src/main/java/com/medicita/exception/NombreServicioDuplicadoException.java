package com.medicita.exception;

public class NombreServicioDuplicadoException extends RuntimeException {

    public NombreServicioDuplicadoException() {
        super("Ya existe un servicio con ese nombre");
    }
}