package io.sqm.playground.api;

/**
 * Identifies the severity level of a playground diagnostic.
 */
public enum DiagnosticSeverityDto {
    error,
    warning,
    info;

    /**
     * Resolves a severity from its lowercase API value.
     *
     * @param value lowercase API value
     * @return matching severity
     */
    public static DiagnosticSeverityDto fromValue(String value) {
        try {
            return DiagnosticSeverityDto.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown diagnostic severity: " + value, e);
        }
    }
}
