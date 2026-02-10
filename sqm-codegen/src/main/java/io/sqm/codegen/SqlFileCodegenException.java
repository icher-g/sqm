package io.sqm.codegen;

/**
 * Indicates code generation failures, including parse diagnostics.
 */
public final class SqlFileCodegenException extends RuntimeException {
    /**
     * Creates an exception with the provided diagnostic message.
     *
     * @param message human-readable diagnostic.
     */
    public SqlFileCodegenException(String message) {
        super(message);
    }
}
