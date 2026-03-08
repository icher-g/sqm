package io.sqm.parser.ansi;

import io.sqm.core.Predicate;
import io.sqm.core.Table;
import io.sqm.core.UpdateStatement;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses baseline ANSI {@code UPDATE} statements.
 */
public class UpdateStatementParser implements Parser<UpdateStatement> {

    /**
     * Creates an update-statement parser.
     */
    public UpdateStatementParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends UpdateStatement> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected UPDATE", TokenType.UPDATE);

        var table = ctx.parse(Table.class, cur);
        if (table.isError()) {
            return error(table);
        }

        cur.expect("Expected SET after UPDATE table", TokenType.SET);

        var assignmentsResult = parseItems(io.sqm.core.Assignment.class, cur, ctx);
        if (assignmentsResult.isError()) {
            return error(assignmentsResult);
        }

        Predicate where = null;
        if (cur.consumeIf(TokenType.WHERE)) {
            var whereResult = ctx.parse(Predicate.class, cur);
            if (whereResult.isError()) {
                return error(whereResult);
            }
            where = whereResult.value();
        }

        return ok(UpdateStatement.of(table.value(), assignmentsResult.value(), where));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<UpdateStatement> targetType() {
        return UpdateStatement.class;
    }
}
