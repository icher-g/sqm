package io.sqm.playground.rest.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps runtime failures to stable playground REST error contracts.
 */
@RestControllerAdvice
public final class PlaygroundExceptionHandler {

    /**
     * Creates playground exception handler.
     */
    public PlaygroundExceptionHandler() {
    }

    /**
     * Handles stable request exceptions.
     *
     * @param exception request exception
     * @param request HTTP request
     * @return response entity with stable error payload
     */
    @ExceptionHandler(PlaygroundRequestException.class)
    public ResponseEntity<PlaygroundErrorResponse> handlePlaygroundRequestException(
        PlaygroundRequestException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.status())
            .body(new PlaygroundErrorResponse(exception.code(), exception.getMessage(), request.getRequestURI()));
    }

    /**
     * Handles malformed request payloads.
     *
     * @param request HTTP request
     * @return response entity with stable error payload
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<PlaygroundErrorResponse> handleUnreadablePayload(
        @SuppressWarnings("unused") HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new PlaygroundErrorResponse("INVALID_REQUEST", "Malformed request payload", request.getRequestURI()));
    }

    /**
     * Handles request validation failures not mapped by explicit request exceptions.
     *
     * @param exception validation exception
     * @param request HTTP request
     * @return response entity with stable invalid-request payload
     */
    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    public ResponseEntity<PlaygroundErrorResponse> handleValidationException(
        RuntimeException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new PlaygroundErrorResponse("INVALID_REQUEST", exception.getMessage(), request.getRequestURI()));
    }

    /**
     * Handles unclassified server failures.
     *
     * @param exception server exception
     * @param request HTTP request
     * @return response entity with stable error payload
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<PlaygroundErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new PlaygroundErrorResponse("INTERNAL_ERROR", exception.getMessage(), request.getRequestURI()));
    }
}
