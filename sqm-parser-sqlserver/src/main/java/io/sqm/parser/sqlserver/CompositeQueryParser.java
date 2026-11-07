package io.sqm.parser.sqlserver;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.parser.AtomicQueryParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Parses SQL Server set queries and enforces result-level pagination ordering.
 */
public class CompositeQueryParser extends io.sqm.parser.ansi.CompositeQueryParser {
    /**
     * Creates a SQL Server composite-query parser.
     */
    public CompositeQueryParser() {
        super(new AtomicQueryParser());
    }

    /**
     * Parses a query and requires ORDER BY for composite OFFSET/FETCH.
     *
     * @param cur token cursor
     * @param ctx dialect parse context
     * @return parsed query or a pagination error
     */
    @Override
    public ParseResult<? extends Query> parse(Cursor cur, ParseContext ctx) {
        var result = super.parse(cur, ctx);
        if (result.ok() && result.value() instanceof CompositeQuery query
            && query.limitOffset() != null && query.limitOffset().offset() != null
            && query.orderBy() == null) {
            return error("OFFSET/FETCH requires ORDER BY in SQL Server", cur.fullPos());
        }
        return result;
    }
}
