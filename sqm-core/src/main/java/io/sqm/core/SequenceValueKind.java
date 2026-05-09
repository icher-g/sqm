package io.sqm.core;

/**
 * Identifies which value is requested from a SQL sequence.
 */
public enum SequenceValueKind {
    /**
     * Requests the next generated sequence value.
     */
    NEXT_VALUE,
    /**
     * Requests the current sequence value in the session.
     */
    CURRENT_VALUE
}
