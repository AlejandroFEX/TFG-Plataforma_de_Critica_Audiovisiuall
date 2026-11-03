package es.udc.fic.fictionary.common;

import org.springframework.http.HttpStatus;

/** La petición no es válida por algo que el cliente puede corregir (400). */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
