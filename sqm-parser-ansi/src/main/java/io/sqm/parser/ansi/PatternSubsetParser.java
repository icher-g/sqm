package io.sqm.parser.ansi;

import io.sqm.core.PatternSubset;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses one named {@code SUBSET} item.
 */
public class PatternSubsetParser implements Parser<PatternSubset> {
    /**
     * Creates a pattern-subset parser.
     */
    public PatternSubsetParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PatternSubset> parse(Cursor cur, ParseContext ctx) {
        var name = toIdentifier(cur.expect("Expected subset name", TokenType.IDENT));
        var equals = cur.expect("Expected = after subset name", TokenType.OPERATOR);
        if (!"=".equals(equals.lexeme())) {
            return io.sqm.parser.spi.ParseResult.error("Expected = after subset name", equals.pos());
        }
        cur.expect("Expected ( after subset =", TokenType.LPAREN);
        var variables = parseIdentifierItems(cur, "Expected pattern variable in subset");
        cur.expect("Expected ) after subset variables", TokenType.RPAREN);
        return ok(PatternSubset.of(name, variables));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternSubset> targetType() {
        return PatternSubset.class;
    }
}
