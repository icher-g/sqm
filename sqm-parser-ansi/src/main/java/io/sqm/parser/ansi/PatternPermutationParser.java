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
 * Parses a {@code PERMUTE(...)} row-pattern primary.
 */
public class PatternPermutationParser implements MatchableParser<MatchPattern.Permutation> {
    /**
     * Creates a pattern-permutation parser.
     */
    public PatternPermutationParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchPattern.Permutation> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected PERMUTE", TokenType.PERMUTE);
        cur.expect("Expected ( after PERMUTE", TokenType.LPAREN);
        var elements = parseItems(MatchPattern.class, cur, ctx);
        if (elements.isError()) return error(elements);
        cur.expect("Expected ) after PERMUTE", TokenType.RPAREN);
        if (elements.value().size() < 2) return error("PERMUTE requires at least two elements", cur.fullPos());
        return ok(MatchPattern.Permutation.of(elements.value()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Permutation> targetType() {
        return MatchPattern.Permutation.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.PERMUTE);
    }
}
