package io.sqm.parser.oracle;

import io.sqm.core.PatternRecognitionTable;
import io.sqm.core.RowsPerMatch;
import io.sqm.core.TableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Applies Oracle-specific option restrictions to the shared row-pattern parser.
 */
public final class PatternRecognitionTableParser extends io.sqm.parser.ansi.PatternRecognitionTableParser {
    /**
     * Creates an Oracle row-pattern relation parser.
     */
    public PatternRecognitionTableParser() {
    }

    /** {@inheritDoc} */
    @Override
    public ParseResult<PatternRecognitionTable> parse(TableRef source, Cursor cur, ParseContext ctx) {
        var parsed = super.parse(source, cur, ctx);
        if (parsed.isError()) {
            return error(parsed);
        }
        if (parsed.value().rowsPerMatch().emptyMatchHandling() != RowsPerMatch.EmptyMatchHandling.DEFAULT) {
            return error(
                "Oracle MATCH_RECOGNIZE does not support explicit empty or unmatched-row handling",
                cur.fullPos()
            );
        }
        return parsed;
    }
}
