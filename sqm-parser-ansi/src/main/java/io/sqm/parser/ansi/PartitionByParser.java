package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.PartitionBy;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses {@code PARTITION BY} clauses.
 */
public class PartitionByParser implements Parser<PartitionBy> {
    /**
     * Creates a partition-by parser.
     */
    public PartitionByParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<PartitionBy> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected PARTITION", TokenType.PARTITION);
        cur.expect("Expected BY after PARTITION", TokenType.BY);
        List<Expression> items = new ArrayList<>();
        do {
            var or = ctx.parse(Expression.class, cur);
            if (or.isError()) {
                return error(or);
            }
            items.add(or.value());
        }
        while (cur.consumeIf(TokenType.COMMA));
        return ok(PartitionBy.of(items));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<PartitionBy> targetType() {
        return PartitionBy.class;
    }
}
