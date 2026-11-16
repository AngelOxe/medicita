package com.medicita.exception;

public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException() {
        super("El correo ya esta registrado");
    }
}