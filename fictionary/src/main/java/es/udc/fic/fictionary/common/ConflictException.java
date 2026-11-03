package es.udc.fic.fictionary.common;

import org.springframework.http.HttpStatus;

/** La operación choca con el estado actual: duplicados (409). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
