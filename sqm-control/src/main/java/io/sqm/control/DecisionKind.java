package io.sqm.control;

/**
 * Final decision returned by SQL middleware processing.
 */
public enum DecisionKind {
    /**
     * SQL passed validation and policy checks.
     */
    ALLOW,

    /**
     * SQL was rejected and must not be executed.
     */
    DENY,

    /**
     * SQL is accepted only after rewrite.
     */
    REWRITE
}
