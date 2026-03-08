package io.sqm.parser.ansi;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Predicate;
import io.sqm.core.Table;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses baseline ANSI {@code DELETE} statements.
 */
public class DeleteStatementParser implements Parser<DeleteStatement> {

    /**
     * Creates a delete-statement parser.
     */
    public DeleteStatementParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends DeleteStatement> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected DELETE", TokenType.DELETE);
        cur.expect("Expected FROM after DELETE", TokenType.FROM);

        var tableResult = ctx.parse(Table.class, cur);
        if (tableResult.isError()) {
            return error(tableResult);
        }

        Predicate where = null;

        if (cur.consumeIf(TokenType.WHERE)) {
            var whereResult = ctx.parse(Predicate.class, cur);
            if (whereResult.isError()) {
                return error(whereResult);
            }
            where = whereResult.value();
        }

        return ok(DeleteStatement.of(tableResult.value(), where));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<DeleteStatement> targetType() {
        return DeleteStatement.class;
    }
}
