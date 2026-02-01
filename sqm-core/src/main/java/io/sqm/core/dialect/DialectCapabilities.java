package io.sqm.core.dialect;

/**
 * Describes which SQL features are supported by a dialect.
 * <p>
 * Parsers and renderers should consult these capabilities to decide
 * whether a construct is accepted or should be rejected as unsupported.
 */
public interface DialectCapabilities {
    /**
     * Returns {@code true} if the dialect supports the given feature.
     *
     * @param feature feature to check
     * @return {@code true} if supported, {@code false} otherwise
     */
    boolean supports(SqlFeature feature);
}
