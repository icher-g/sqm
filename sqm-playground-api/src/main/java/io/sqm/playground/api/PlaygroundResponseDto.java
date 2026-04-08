package io.sqm.playground.api;

import java.util.List;

/**
 * Common response contract implemented by playground operation DTOs.
 */
public interface PlaygroundResponseDto {

    /**
     * Returns the request identifier used for correlation and debugging.
     *
     * @return request identifier
     */
    String requestId();

    /**
     * Returns whether the operation completed successfully.
     *
     * @return {@code true} when the operation completed successfully
     */
    boolean success();

    /**
     * Returns the operation duration in milliseconds.
     *
     * @return operation duration in milliseconds
     */
    long durationMs();

    /**
     * Returns structured diagnostics emitted by the operation.
     *
     * @return operation diagnostics
     */
    List<PlaygroundDiagnosticDto> diagnostics();
}
