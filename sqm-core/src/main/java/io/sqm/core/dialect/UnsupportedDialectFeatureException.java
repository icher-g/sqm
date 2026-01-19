package io.sqm.core.dialect;

/**
 * Thrown when a SQL dialect cannot parse or render a specific
 * SQM construct.
 */
public final class UnsupportedDialectFeatureException extends RuntimeException {

    public UnsupportedDialectFeatureException(String message) {
        super(message);
    }

    public UnsupportedDialectFeatureException(String feature, String dialect) {
        super("Feature not supported by dialect [" + dialect + "]: " + feature);
    }
}

