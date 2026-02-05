package io.sqm.parser.spi;

import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Token;

/**
 * Defines dialect-specific rules for classifying and handling SQL operators
 * during parsing.
 * <p>
 * An {@code OperatorPolicy} allows a SQL dialect to influence how operator
 * tokens are interpreted by generic parsers, without hardcoding operator
 * semantics or precedence rules into shared parsing logic.
 * <p>
 * Typical use cases include:
 * <ul>
 *   <li>Excluding certain operators from generic binary operator parsing so
 *       they can be handled by higher-precedence or specialized parsers
 *       (for example exponentiation in PostgreSQL).</li>
 *   <li>Enabling or disabling operator families based on supported
 *       {@link SqlFeature SQL features}.</li>
 *   <li>Resolving ambiguities where the same operator token has different
 *       meanings in different SQL dialects.</li>
 * </ul>
 * <p>
 * This interface is intentionally minimal and declarative. It does not
 * perform parsing itself, but provides guidance to parsers such as
 * {@code BinaryOperatorExprParser} when deciding whether an operator token
 * should be consumed at a given precedence level.
 * <p>
 * Implementations are expected to be dialect-specific and typically exposed
 * via a dialect {@code Specs} or equivalent configuration object.
 */
public interface OperatorPolicy {

    /**
     * Determines whether the given token should be treated as a generic binary
     * operator by low-precedence operator parsers.
     * <p>
     * If this method returns {@code false}, the operator token is expected to be
     * handled by a higher-precedence or specialized parser, or rejected entirely
     * if unsupported by the dialect.
     * <p>
     * The provided feature set allows implementations to enable or disable
     * operator behavior based on dialect capabilities.
     *
     * @param token the operator token at the current parse position
     * @return {@code true} if the token should be parsed as a generic binary
     * operator; {@code false} otherwise
     */
    boolean isGenericBinaryOperator(Token token);

    /**
     * Returns the precedence tier for a custom operator.
     * <p>
     * Dialects may override this to model operator precedence tiers for
     * non-built-in operators. The default implementation assigns all custom
     * operators to the same (lowest) tier.
     *
     * @param operator operator text (for example {@code "->"})
     * @return precedence tier for the custom operator
     */
    default OperatorPrecedence customOperatorPrecedence(String operator) {
        return OperatorPrecedence.CUSTOM_LOW;
    }
}

