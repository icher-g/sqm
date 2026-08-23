package io.sqm.parser.ansi;

import io.sqm.core.ClassifierExpr;
import io.sqm.core.Identifier;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses the scoped {@code CLASSIFIER(...)} expression.
 */
public class ClassifierExprParser implements MatchableParser<ClassifierExpr> {
    /**
     * Creates a classifier-expression parser.
     */
    public ClassifierExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ClassifierExpr> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected CLASSIFIER", TokenType.IDENT);
        cur.expect("Expected ( after CLASSIFIER", TokenType.LPAREN);
        Identifier variable = cur.match(TokenType.IDENT) ? toIdentifier(cur.advance()) : null;
        cur.expect("Expected ) after CLASSIFIER", TokenType.RPAREN);
        return ok(ClassifierExpr.of(variable));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ClassifierExpr> targetType() {
        return ClassifierExpr.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.IDENT, "CLASSIFIER") && cur.match(TokenType.LPAREN, 1);
    }
}
