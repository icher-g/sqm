package io.sqm.parser.sqlserver;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;

/**
 * Parses SQL Server TABLESAMPLE syntax.
 */
public class SampledTableParser extends io.sqm.parser.ansi.SampledTableParser {
    /**
     * Creates a SQL Server sampled-table parser.
     */
    public SampledTableParser() {
    }

    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.TABLESAMPLE);
    }
}
