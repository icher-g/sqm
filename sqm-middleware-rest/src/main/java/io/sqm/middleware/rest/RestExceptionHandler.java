package io.sqm.middleware.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps runtime failures to stable REST error contracts.
 */
@RestControllerAdvice
public final class RestExceptionHandler {

    /**
     * Handles stable request exceptions.
     *
     * @param exception request exception
     * @param request HTTP request
     * @return response entity with stable error payload
     */
    @ExceptionHandler(RestRequestException.class)
    public ResponseEntity<RestErrorResponse> handleRestRequestException(
        RestRequestException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.status())
            .body(new RestErrorResponse(exception.code(), exception.getMessage(), request.getRequestURI()));
    }

    /**
     * Handles malformed request payloads.
     *
     * @param exception payload conversion exception
     * @param request HTTP request
     * @return response entity with stable error payload
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestErrorResponse> handleUnreadablePayload(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new RestErrorResponse("INVALID_REQUEST", "Malformed request payload", request.getRequestURI()));
    }

    /**
     * Handles request validation failures not mapped by explicit request exceptions.
     *
     * @param exception validation exception
     * @param request HTTP request
     * @return response entity with stable invalid-request payload
     */
    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    public ResponseEntity<RestErrorResponse> handleValidationException(
        RuntimeException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new RestErrorResponse("INVALID_REQUEST", exception.getMessage(), request.getRequestURI()));
    }

    /**
     * Handles unclassified server failures.
     *
     * @param exception server exception
     * @param request HTTP request
     * @return response entity with stable error payload
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new RestErrorResponse("INTERNAL_ERROR", exception.getMessage(), request.getRequestURI()));
    }
}
