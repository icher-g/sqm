package io.sqm.core.dialect;

/**
 * Thrown when a SQL dialect cannot parse or render a specific
 * SQM construct.
 */
public final class UnsupportedDialectFeatureException extends RuntimeException {

    /**
     * Creates an exception with a custom message.
     *
     * @param message error message
     */
    public UnsupportedDialectFeatureException(String message) {
        super(message);
    }

    /**
     * Creates an exception for an unsupported feature and dialect.
     *
     * @param feature unsupported feature description
     * @param dialect dialect name
     */
    public UnsupportedDialectFeatureException(String feature, String dialect) {
        super("Feature not supported by dialect [" + dialect + "]: " + feature);
    }
}

