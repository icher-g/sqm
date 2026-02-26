package io.sqm.middleware.api;

/**
 * Transport-neutral decision kind.
 */
public enum DecisionKindDto {
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
