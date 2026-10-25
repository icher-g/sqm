package io.sqm.parser.ansi;

import io.sqm.core.IterationSpec;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for IterationSpec. Oracle support follows in #506.
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
        return ParseResult.error(ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL IterationSpec is not supported yet (#506)" : "MODEL IterationSpec is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IterationSpec> targetType() {
        return IterationSpec.class;
    }
}
