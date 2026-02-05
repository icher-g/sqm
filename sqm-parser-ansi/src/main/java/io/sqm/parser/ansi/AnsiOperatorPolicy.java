package io.sqm.parser.ansi;

import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.OperatorPolicy;

import static io.sqm.parser.core.OperatorTokens.*;

public class AnsiOperatorPolicy implements OperatorPolicy {
    private static boolean isBinaryOperatorToken(Token t) {
        return t.type() == TokenType.OPERATOR || t.type() == TokenType.QMARK;
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
        return isBinaryOperatorToken(token) && !isArithmetic(token) && !isComparison(token) && !isRegex(token);
    }
}
