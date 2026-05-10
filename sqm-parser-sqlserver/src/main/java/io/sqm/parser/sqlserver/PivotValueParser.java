package io.sqm.parser.sqlserver;

import io.sqm.core.Identifier;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * SQL Server parser for {@code PIVOT} values.
 */
public class PivotValueParser extends io.sqm.parser.ansi.PivotValueParser {
    /**
     * Creates a SQL Server pivot value parser.
     */
    public PivotValueParser() {
    }

    @Override
    protected ParseResult<Identifier> parseAlias(Cursor cur) {
        if (cur.match(TokenType.AS) || cur.match(TokenType.IDENT)) {
            return error("SQL Server PIVOT value aliases are not supported", cur.fullPos());
        }
        return ok(null);
    }
}
