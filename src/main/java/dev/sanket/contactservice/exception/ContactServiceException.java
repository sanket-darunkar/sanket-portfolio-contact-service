package dev.sanket.contactservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Base runtime exception for all contact-service business errors.
 * Carries an HTTP status so the global handler can map it directly.
 */
public class ContactServiceException extends RuntimeException {

    private final HttpStatus status;

    public ContactServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ContactServiceException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
