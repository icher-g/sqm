package io.sqm.parser.sqlserver;

import io.sqm.core.OutputClause;
import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputRowSource;
import io.sqm.parser.ansi.DeleteStatementParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL Server {@code DELETE} statements with {@code OUTPUT}.
 */
public class SqlServerDeleteStatementParser extends DeleteStatementParser {

    /**
     * Creates a SQL Server delete parser.
     */
    public SqlServerDeleteStatementParser() {
    }

    /**
     * Parses optional {@code OUTPUT} clause.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed output clause or {@code null} when omitted
     */
    @Override
    protected ParseResult<OutputClause> parseOutput(Cursor cur, ParseContext ctx) {
        if (!cur.match(io.sqm.parser.core.TokenType.OUTPUT)) {
            return ok(null);
        }

        var output = ctx.parse(OutputClause.class, cur);
        if (output.isError()) {
            return error(output);
        }

        for (var item : output.value().items()) {
            if (item.expression() instanceof OutputColumnExpr outputColumn && outputColumn.source() == OutputRowSource.INSERTED) {
                return error("DELETE ... OUTPUT does not support inserted.<column> references", cur.fullPos());
            }
        }

        return ok(output.value());
    }
}