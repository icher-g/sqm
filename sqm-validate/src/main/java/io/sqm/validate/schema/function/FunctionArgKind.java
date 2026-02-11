package io.sqm.validate.schema.function;

/**
 * Declares expected argument kind for function signature validation.
 */
public enum FunctionArgKind {
    /**
     * Any expression argument.
     */
    ANY_EXPR,
    /**
     * String-compatible expression argument.
     */
    STRING_EXPR,
    /**
     * Numeric-compatible expression argument.
     */
    NUMERIC_EXPR,
    /**
     * Either expression argument or {@code *} argument.
     */
    STAR_OR_EXPR
}

