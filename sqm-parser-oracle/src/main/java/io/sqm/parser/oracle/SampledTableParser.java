package io.sqm.parser.oracle;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;

/**
 * Parses Oracle table sampling syntax.
 */
public class SampledTableParser extends io.sqm.parser.ansi.SampledTableParser {
    /**
     * Creates an Oracle sampled-table parser.
     */
    public SampledTableParser() {
    }

    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.SAMPLE);
    }
}
