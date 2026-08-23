package io.sqm.parser.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a partition-start or partition-end row-pattern anchor.
 */
public class PatternAnchorParser implements MatchableParser<MatchPattern.Anchor> {
    /**
     * Creates a pattern-anchor parser.
     */
    public PatternAnchorParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchPattern.Anchor> parse(Cursor cur, ParseContext ctx) {
        var kind = cur.consumeIf(t -> t.type() == TokenType.OPERATOR && "^".equals(t.lexeme()))
            ? MatchPattern.Anchor.Kind.START : MatchPattern.Anchor.Kind.END;
        if (kind == MatchPattern.Anchor.Kind.END) cur.expect("Expected $", TokenType.DOLLAR);
        return ok(MatchPattern.Anchor.of(kind));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Anchor> targetType() {
        return MatchPattern.Anchor.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.DOLLAR) || PatternGrammar.isOperator(cur, "^");
    }
}
