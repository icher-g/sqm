package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.PivotValue;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses an explicit {@link PivotValue} inside a {@code PIVOT} clause.
 */
public class PivotValueParser implements Parser<PivotValue> {
    /**
     * Creates a pivot value parser.
     */
    public PivotValueParser() {
    }

    /**
     * Parses an explicit pivot value.
     *
     * @param cur cursor positioned at the pivot value expression
     * @param ctx parse context
     * @return parsed pivot value
     */
    @Override
    public ParseResult<? extends PivotValue> parse(Cursor cur, ParseContext ctx) {
        var value = ctx.parse(Expression.class, cur);
        if (value.isError()) {
            return error(value);
        }
        var alias = parseAlias(cur);
        if (alias.isError()) {
            return error(alias);
        }
        return ok(PivotValue.of(value.value(), alias.value()));
    }

    /**
     * Gets the parser target type.
     *
     * @return pivot value type
     */
    @Override
    public Class<PivotValue> targetType() {
        return PivotValue.class;
    }

    /**
     * Parses an optional pivot value alias.
     *
     * @param cur cursor positioned after the pivot value expression
     * @return parsed alias or {@code null}
     */
    protected ParseResult<Identifier> parseAlias(Cursor cur) {
        return ok(parseAliasIdentifier(cur));
    }
}
