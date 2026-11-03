package es.udc.fic.fictionary.common;

import org.springframework.http.HttpStatus;

/** Error de negocio con código HTTP propio. El manejador global lo convierte en respuesta JSON. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
