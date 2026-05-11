package io.sqm.parser.sqlserver;

import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * SQL Server parser for {@code UNPIVOT} inputs.
 */
public class UnpivotInputParser extends io.sqm.parser.ansi.UnpivotInputParser {
    /**
     * Creates a SQL Server unpivot input parser.
     */
    public UnpivotInputParser() {
    }

    @Override
    protected ParseResult<Expression> parseLabel(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.AS)) {
            return error("SQL Server UNPIVOT input labels are not supported", cur.fullPos());
        }
        return ok(null);
    }
}
