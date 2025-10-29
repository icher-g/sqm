package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class CompositeQueryParser implements Parser<CompositeQuery> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<CompositeQuery> parse(Cursor cur, ParseContext ctx) {
        List<Query> terms = new ArrayList<>();
        List<SetOperator> ops = new ArrayList<>();

        Token t;
        do {
            // extract a sub query to avoid recursive parsing.
            var queryCur = cur.advance(cur.find(Terminators.COMPOSITE_QUERY));
            queryCur.consumeIf(TokenType.LPAREN); // remove '(' if wrapped.
            var term = ctx.parse(Query.class, queryCur);
            if (term.isError()) {
                return error(term);
            }
            terms.add(term.value());
            t = cur.isEof() || cur.match(TokenType.RPAREN) ? cur.peek() : cur.advance();
            var isAll = cur.consumeIf(TokenType.ALL);
            switch (t.type()) {
                case UNION -> {
                    if (isAll)
                        ops.add(SetOperator.UNION_ALL);
                    else
                        ops.add(SetOperator.UNION);
                }
                case INTERSECT -> {
                    if (isAll)
                        ops.add(SetOperator.INTERSECT_ALL);
                    else
                        ops.add(SetOperator.INTERSECT);
                }
                case EXCEPT -> {
                    if (isAll)
                        ops.add(SetOperator.EXCEPT_ALL);
                    else
                        ops.add(SetOperator.EXCEPT);
                }
            }
        } while (Indicators.COMPOSITE_QUERY.contains(t.type()));

        // ORDER BY (optional)
        OrderBy orderBy = null;
        if (cur.match(TokenType.ORDER) && cur.match(TokenType.BY, 1)) {
            var obr = ctx.parse(OrderBy.class, cur);
            if (obr.isError()) {
                return error(obr);
            }
            orderBy = obr.value();
        }

        // LIMIT & OFFSET (optional)
        var lor = ctx.parse(LimitOffset.class, cur);
        if (lor.isError()) {
            return error(lor);
        }

        return ok(Query.compose(terms, ops, orderBy, lor.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CompositeQuery> targetType() {
        return CompositeQuery.class;
    }
}
