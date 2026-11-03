package es.udc.fic.fictionary.common;

import org.springframework.http.HttpStatus;

/** El recurso pedido no existe (404). */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
