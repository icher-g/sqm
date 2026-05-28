package io.sqm.parser.postgresql;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;

/**
 * Parses PostgreSQL TABLESAMPLE syntax.
 */
public class SampledTableParser extends io.sqm.parser.ansi.SampledTableParser {
    /**
     * Creates a PostgreSQL sampled-table parser.
     */
    public SampledTableParser() {
    }

    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.TABLESAMPLE);
    }
}
