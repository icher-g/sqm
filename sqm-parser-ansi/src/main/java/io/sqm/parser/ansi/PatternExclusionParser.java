package io.sqm.parser.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a row-pattern exclusion {@code {- pattern -}}.
 */
public class PatternExclusionParser implements MatchableParser<MatchPattern.Exclusion> {
    /**
     * Creates a pattern-exclusion parser.
     */
    public PatternExclusionParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchPattern.Exclusion> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected { for exclusion", TokenType.LBRACE);
        if (!PatternGrammar.isOperator(cur, "-")) return error("Expected - after {", cur.fullPos());
        cur.advance();
        var pattern = ctx.parse(MatchPattern.class, cur);
        if (pattern.isError()) return error(pattern);
        if (!PatternGrammar.isOperator(cur, "-")) return error("Expected - before }", cur.fullPos());
        cur.advance();
        cur.expect("Expected } after exclusion", TokenType.RBRACE);
        return ok(MatchPattern.Exclusion.of(pattern.value()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Exclusion> targetType() {
        return MatchPattern.Exclusion.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.LBRACE) && PatternGrammar.isOperator(cur, "-", 1);
    }
}
