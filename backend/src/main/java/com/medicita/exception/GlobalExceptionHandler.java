package com.medicita.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
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
        @ExceptionHandler(CorreoDuplicadoException.class)
public ResponseEntity<ApiError> manejarCorreoDuplicado(
        CorreoDuplicadoException exception,
        HttpServletRequest request) {

    return responder(
            HttpStatus.valueOf(422),
            exception.getMessage(),
            request,
            List.of(
                    new ApiError.FieldError(
                            "email",
                            exception.getMessage()
                    )
            )
    );
}
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException exception,
            HttpServletRequest request) {

        return responder(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(NombreServicioDuplicadoException.class)
    public ResponseEntity<ApiError> manejarNombreServicioDuplicado(
            NombreServicioDuplicadoException exception,
            HttpServletRequest request) {

        return responder(
                HttpStatus.valueOf(422),
                exception.getMessage(),
                request,
                List.of(
                        new ApiError.FieldError(
                                "nombre",
                                exception.getMessage()
                        )
                )
        );
    }

    // ---------- 422: reglas de negocio ----------

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> manejarReglaNegocio(
            ReglaNegocioException exception,
            HttpServletRequest request) {

        List<ApiError.FieldError> errores = exception.getCampo() == null
                ? List.of()
                : List.of(new ApiError.FieldError(
                        exception.getCampo(),
                        exception.getMessage()));

        return responder(
                HttpStatus.valueOf(422),
                exception.getMessage(),
                request,
                errores
        );
    }

    /**
     * La base de datos rechazo la operacion. Se revisa el SQLState para saber por que:
     * solo 23P01 significa "cita traslapada"; un valor repetido (23505) es otro error.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> manejarIntegridad(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {

        String sqlState = obtenerSqlState(exception);

        if ("23P01".equals(sqlState)) {
            return responder(
                    HttpStatus.valueOf(422),
                    "El horario seleccionado ya no esta disponible",
                    request,
                    List.of(new ApiError.FieldError("inicio", "Horario ocupado"))
            );
        }
        if ("23505".equals(sqlState)) {
            return responder(
                    HttpStatus.valueOf(422),
                    "Ya existe un registro con esos datos",
                    request,
                    List.of()
            );
        }
        if ("23503".equals(sqlState)) {
            return responder(
                    HttpStatus.valueOf(422),
                    "No se puede completar la operacion porque hay registros relacionados",
                    request,
                    List.of()
            );
        }

        log.error("Error de integridad no esperado", exception);
        return responder(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error interno",
                request,
                List.of()
        );
    }

    /**
     * Dos reservas casi simultaneas pueden provocar un deadlock en PostgreSQL (SQLState 40P01)
     * en lugar del error de restriccion 23P01: la base aborta a una de las dos. Para el usuario
     * es lo mismo que un empalme: otra persona se quedo con el horario.
     */
    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<ApiError> manejarBloqueoConcurrente(
            CannotAcquireLockException exception,
            HttpServletRequest request) {

        return responder(
                HttpStatus.valueOf(422),
                "El horario seleccionado ya no esta disponible",
                request,
                List.of(new ApiError.FieldError("inicio", "Horario ocupado"))
        );
    }

    // ---------- 400: parametros de la URL ----------

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> manejarTipoIncorrecto(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        return responder(
                HttpStatus.BAD_REQUEST,
                "Valor invalido para el parametro '" + exception.getName() + "'",
                request,
                List.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> manejarParametroFaltante(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {

        return responder(
                HttpStatus.BAD_REQUEST,
                "Falta el parametro '" + exception.getParameterName() + "'",
                request,
                List.of()
        );
    }

    /** Recorre la cadena de causas hasta encontrar la SQLException original. */
    private String obtenerSqlState(Throwable exception) {
        for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return null;
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