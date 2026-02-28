package io.sqm.middleware.core;

import java.util.Objects;

/**
 * Runtime schema bootstrap status used for startup diagnostics and readiness checks.
 *
 * @param state       schema bootstrap state
 * @param source      configured schema source
 * @param description operator-facing source description
 * @param error       optional schema bootstrap error message
 */
public record SchemaBootstrapStatus(
    State state,
    String source,
    String description,
    String error
) {

    /**
     * Creates a ready schema bootstrap status.
     *
     * @param source      configured schema source
     * @param description source description
     * @return ready status
     */
    public static SchemaBootstrapStatus ready(String source, String description) {
        return new SchemaBootstrapStatus(State.READY, requireText(source, "source"), requireText(description, "description"), null);
    }

    /**
     * Creates a degraded schema bootstrap status.
     *
     * @param source      configured schema source
     * @param description source description
     * @param error       bootstrap error message
     * @return degraded status
     */
    public static SchemaBootstrapStatus degraded(String source, String description, String error) {
        return new SchemaBootstrapStatus(
            State.DEGRADED,
            requireText(source, "source"),
            requireText(description, "description"),
            requireText(error, "error")
        );
    }

    /**
     * Returns whether schema bootstrap is ready for normal request handling.
     *
     * @return true when ready
     */
    public boolean ready() {
        return state == State.READY;
    }

    /**
     * Runtime schema bootstrap state.
     */
    public enum State {
        /**
         * Schema loaded successfully.
         */
        READY,
        /**
         * Schema bootstrap failed and runtime started in degraded mode.
         */
        DEGRADED
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

