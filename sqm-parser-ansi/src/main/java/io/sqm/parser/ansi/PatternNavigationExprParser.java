package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.PatternNavigationExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.Locale;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses scoped row-pattern navigation expressions.
 */
public class PatternNavigationExprParser implements MatchableParser<PatternNavigationExpr> {
    /**
     * Creates a pattern-navigation parser.
     */
    public PatternNavigationExprParser() {
    }

    private static boolean isName(Cursor cur, int offset) {
        if (cur.matchAny(offset, TokenType.FIRST, TokenType.LAST, TokenType.NEXT)) return true;
        return cur.match(TokenType.IDENT, "PREV", offset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PatternNavigationExpr> parse(Cursor cur, ParseContext ctx) {
        Token operation = cur.advance();
        var kind = PatternNavigationExpr.Kind.valueOf(operation.lexeme().toUpperCase(Locale.ROOT));
        cur.expect("Expected ( after pattern navigation function", TokenType.LPAREN);
        var expression = ctx.parse(Expression.class, cur);
        if (expression.isError()) return error(expression);
        Expression offset = null;
        if (cur.consumeIf(TokenType.COMMA)) {
            var parsedOffset = ctx.parse(Expression.class, cur);
            if (parsedOffset.isError()) return error(parsedOffset);
            offset = parsedOffset.value();
        }
        cur.expect("Expected ) after pattern navigation function", TokenType.RPAREN);
        return ok(PatternNavigationExpr.of(kind, expression.value(), offset));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternNavigationExpr> targetType() {
        return PatternNavigationExpr.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return isName(cur, 0) && cur.match(TokenType.LPAREN, 1);
    }
}
