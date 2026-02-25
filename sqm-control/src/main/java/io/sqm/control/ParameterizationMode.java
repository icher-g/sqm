package io.sqm.control;

/**
 * Configuration policy for SQL literal parameterization in middleware output.
 *
 * <p>This enum intentionally lives in {@code sqm-control} (not renderer modules)
 * so middleware configuration can expose a stable user-facing policy without
 * coupling callers to renderer-specific output contracts yet.</p>
 *
 * <p>The policy defines whether middleware should produce parameterized SQL
 * payloads when that optional mode is enabled in future epics.</p>
 */
public enum ParameterizationMode {
    /**
     * Do not parameterize inline literals.
     */
    OFF,

    /**
     * Parameterize inline literals using dialect placeholders (for example {@code ?}).
     */
    BIND
}
