package io.sqm.parser.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.core.Identifier;
import io.sqm.core.PivotMeasure;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a {@link PivotMeasure} inside a {@code PIVOT} clause.
 */
public class PivotMeasureParser implements Parser<PivotMeasure> {
    /**
     * Creates a pivot measure parser.
     */
    public PivotMeasureParser() {
    }

    /**
     * Parses a pivot aggregate measure.
     *
     * @param cur cursor positioned at the aggregate function
     * @param ctx parse context
     * @return parsed pivot measure
     */
    @Override
    public ParseResult<? extends PivotMeasure> parse(Cursor cur, ParseContext ctx) {
        var aggregate = ctx.parse(FunctionExpr.class, cur);
        if (aggregate.isError()) {
            return error(aggregate);
        }
        var alias = parseAlias(cur);
        if (alias.isError()) {
            return error(alias);
        }
        return ok(PivotMeasure.of(aggregate.value(), alias.value()));
    }

    /**
     * Gets the parser target type.
     *
     * @return pivot measure type
     */
    @Override
    public Class<PivotMeasure> targetType() {
        return PivotMeasure.class;
    }

    /**
     * Parses an optional pivot measure alias.
     *
     * @param cur cursor positioned after the measure expression
     * @return parsed alias or {@code null}
     */
    protected ParseResult<Identifier> parseAlias(Cursor cur) {
        return ok(parseAliasIdentifier(cur));
    }
}
