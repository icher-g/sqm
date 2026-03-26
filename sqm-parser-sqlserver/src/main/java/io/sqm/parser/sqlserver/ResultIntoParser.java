package io.sqm.parser.sqlserver;

import io.sqm.core.Identifier;
import io.sqm.core.ResultInto;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.core.VariableTableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL Server-style {@code OUTPUT ... INTO ...} targets.
 */
public class ResultIntoParser implements Parser<ResultInto> {

    /**
     * Creates a result-into parser.
     */
    public ResultIntoParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ResultInto> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected INTO after OUTPUT projection", TokenType.INTO);

        TableRef target;

        var tableVariable = ctx.parseIfMatch(VariableTableRef.class, cur);
        if (tableVariable.match()) {
            if (tableVariable.result().isError()) {
                return error(tableVariable.result());
            }
            target = tableVariable.result().value();
        }
        else {
            var table = ctx.parse(Table.class, cur);
            if (table.isError()) {
                return error(table);
            }
            if (table.value().hints().stream().anyMatch(h -> switch (h.name().value()) {
                case "NOLOCK", "UPDLOCK", "HOLDLOCK" -> true;
                default -> false;
            })) {
                return error("SQL Server table hints are not supported on OUTPUT INTO targets", cur.fullPos());
            }
            target = table.value();
        }

        List<Identifier> columns = List.of();
        if (cur.consumeIf(TokenType.LPAREN)) {
            columns = parseIdentifierItems(cur, "Expected result target column");
            cur.expect("Expected ) after OUTPUT INTO target columns", TokenType.RPAREN);
        }

        return ok(ResultInto.of(target, columns));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ResultInto> targetType() {
        return ResultInto.class;
    }
}
