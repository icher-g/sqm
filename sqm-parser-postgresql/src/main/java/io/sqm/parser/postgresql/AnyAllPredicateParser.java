package io.sqm.parser.postgresql;

import io.sqm.core.Expression;
import io.sqm.core.QuantifiedSource;
import io.sqm.core.Query;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * PostgreSQL parser for quantified ANY/ALL predicates.
 */
public class AnyAllPredicateParser extends io.sqm.parser.ansi.AnyAllPredicateParser {

    /**
     * Creates a PostgreSQL ANY/ALL predicate parser.
     */
    public AnyAllPredicateParser() {
    }

    /**
     * Parses either the standard query source or PostgreSQL's array-expression source.
     *
     * @param cur the cursor positioned after the opening parenthesis.
     * @param ctx the parse context.
     * @return the parsed quantified source.
     */
    @Override
    protected ParseResult<? extends QuantifiedSource> parseSource(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeQuery(cur)) {
            return ctx.parse(Query.class, cur);
        }
        return ctx.parse(Expression.class, cur);
    }
}
