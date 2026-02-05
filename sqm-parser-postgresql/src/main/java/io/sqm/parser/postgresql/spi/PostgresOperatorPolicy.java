package io.sqm.parser.postgresql.spi;

import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.OperatorPolicy;
import io.sqm.parser.spi.OperatorPrecedence;

import java.util.Locale;

public class PostgresOperatorPolicy implements OperatorPolicy {

    private final OperatorPolicy ansiOperatorPolicy;

    public PostgresOperatorPolicy(OperatorPolicy ansiOperatorPolicy) {
        this.ansiOperatorPolicy = ansiOperatorPolicy;
    }

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
    @Override
    public boolean isGenericBinaryOperator(Token token) {
        return ansiOperatorPolicy.isGenericBinaryOperator(token) && !(token.type() == TokenType.OPERATOR && "^".equals(token.lexeme()));
    }

    /**
     * Provides PostgreSQL-specific precedence tiers for custom operators.
     * <p>
     * The {@code OPERATOR(...)} syntax always uses the lowest custom-precedence
     * tier, regardless of the operator inside. For bare operators, the tier is
     * derived from the leading character to provide predictable binding when
     * mixing multiple custom operators.
     *
     * @param operator operator text (for example {@code "->"})
     * @return precedence tier for the custom operator
     */
    @Override
    public OperatorPrecedence customOperatorPrecedence(String operator) {
        if (operator == null || operator.isEmpty() || operator.toUpperCase(Locale.ROOT).startsWith("OPERATOR")) {
            return OperatorPrecedence.CUSTOM_LOW;
        }
        return switch (operator.charAt(0)) {
            case '|' -> OperatorPrecedence.CUSTOM_LOW;
            case '&' -> OperatorPrecedence.CUSTOM_MEDIUM;
            default -> OperatorPrecedence.CUSTOM_HIGH;
        };
    }
}
