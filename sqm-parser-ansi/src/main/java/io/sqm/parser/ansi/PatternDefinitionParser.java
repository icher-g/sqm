package io.sqm.parser.ansi;

import io.sqm.core.PatternDefinition;
import io.sqm.core.Predicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses one named condition in a {@code DEFINE} clause.
 */
public class PatternDefinitionParser implements Parser<PatternDefinition> {
    /**
     * Creates a pattern-definition parser.
     */
    public PatternDefinitionParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PatternDefinition> parse(Cursor cur, ParseContext ctx) {
        var variable = toIdentifier(cur.expect("Expected pattern variable in DEFINE", TokenType.IDENT));
        cur.expect("Expected AS after DEFINE variable", TokenType.AS);
        var condition = ctx.parse(Predicate.class, cur);
        if (condition.isError()) return error(condition);
        return ok(PatternDefinition.of(variable, condition.value()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternDefinition> targetType() {
        return PatternDefinition.class;
    }
}
