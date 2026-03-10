package io.sqm.parser;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Entry-point parser for SQL statements.
 */
public class StatementParser implements Parser<Statement> {

    /**
     * Creates a statement parser.
     */
    public StatementParser() {
    }

    @Override
    public ParseResult<? extends Statement> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.INSERT) || cur.match(TokenType.REPLACE)) {
            return ctx.parse(InsertStatement.class, cur);
        }
        if (cur.match(TokenType.UPDATE)) {
            return ctx.parse(UpdateStatement.class, cur);
        }
        if (cur.match(TokenType.DELETE)) {
            return ctx.parse(DeleteStatement.class, cur);
        }
        return ctx.parse(Query.class, cur);
    }

    @Override
    public Class<Statement> targetType() {
        return Statement.class;
    }
}