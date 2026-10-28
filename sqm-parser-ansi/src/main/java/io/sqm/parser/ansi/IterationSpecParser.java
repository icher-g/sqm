package io.sqm.parser.ansi;

import io.sqm.core.IterationSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for IterationSpec; supporting dialects override it.
 */
public class IterationSpecParser implements Parser<IterationSpec> {
    /**
     * Creates the IterationSpec parser.
     */
    public IterationSpecParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends IterationSpec> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL IterationSpec is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IterationSpec> targetType() {
        return IterationSpec.class;
    }
}
