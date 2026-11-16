package com.medicita.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidacion(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        var errores = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiError.FieldError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        return responder(
                HttpStatus.BAD_REQUEST,
                "Datos invalidos",
                request,
                errores
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> manejarJsonInvalido(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        return responder(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es valido",
                request,
                List.of()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> manejarAutenticacion(
            AuthenticationException exception,
            HttpServletRequest request) {

        return responder(
                HttpStatus.UNAUTHORIZED,
                "Credenciales incorrectas o cuenta no disponible",
                request,
                List.of()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> manejarAccesoDenegado(
            AccessDeniedException exception,
            HttpServletRequest request) {

        return responder(
                HttpStatus.FORBIDDEN,
                "No tienes permiso para realizar esta operacion",
                request,
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarErrorInesperado(
            Exception exception,
            HttpServletRequest request) {

        log.error("Error inesperado al procesar una solicitud", exception);

        return responder(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error interno",
                request,
                List.of()
        );
    }

    private ResponseEntity<ApiError> responder(
            HttpStatus status,
            String mensaje,
            HttpServletRequest request,
            List<ApiError.FieldError> errores) {

        var respuesta = new ApiError(
                Instant.now(),
                status.value(),
                mensaje,
                request.getRequestURI(),
                errores
        );

        return ResponseEntity.status(status).body(respuesta);
    }
}