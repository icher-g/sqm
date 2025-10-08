package io.cherlabs.sqm.render.spi;

/**
 * A list of supported parameterization modes.
 */
public enum ParameterizationMode {
    /**
     * The parameter is inlined in the query.
     */
    Inline,
    /**
     * The parameter is added to the query as a parameter.
     */
    Bind
}
