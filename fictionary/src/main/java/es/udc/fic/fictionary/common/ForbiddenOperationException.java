package es.udc.fic.fictionary.common;

import org.springframework.http.HttpStatus;

/** El usuario está identificado pero no puede hacer esto (403). */
public class ForbiddenOperationException extends ApiException {
    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
