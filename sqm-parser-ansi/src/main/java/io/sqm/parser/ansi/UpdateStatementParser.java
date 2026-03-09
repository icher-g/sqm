package io.sqm.parser.ansi;

import io.sqm.core.Predicate;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.core.UpdateStatement;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.List;

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

        var fromResult = parseFrom(cur, ctx);
        if (fromResult.isError()) {
            return error(fromResult);
        }

        Predicate where = null;

        if (cur.consumeIf(TokenType.WHERE)) {
            var whereResult = ctx.parse(Predicate.class, cur);
            if (whereResult.isError()) {
                return error(whereResult);
            }
            where = whereResult.value();
        }

        return ok(UpdateStatement.of(table.value(), assignmentsResult.value(), fromResult.value(), where));
    }

    /**
     * Parses optional {@code FROM} sources.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed FROM sources or empty list when omitted
     */
    protected ParseResult<List<TableRef>> parseFrom(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.FROM)) {
            return error("UPDATE ... FROM is not supported by this dialect", cur.fullPos());
        }
        return ok(List.of());
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
