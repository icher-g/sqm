package io.sqm.parser.postgresql;

import io.sqm.core.Assignment;
import io.sqm.core.MergeUpdateAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses PostgreSQL {@code MERGE ... WHEN MATCHED THEN UPDATE SET ...} actions.
 */
public class MergeUpdateActionParser extends io.sqm.parser.ansi.MergeUpdateActionParser {

    /**
     * Creates a PostgreSQL merge-update-action parser.
     */
    public MergeUpdateActionParser() {
    }

    @Override
    public ParseResult<? extends MergeUpdateAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected UPDATE", TokenType.UPDATE);
        cur.expect("Expected SET after MERGE UPDATE action", TokenType.SET);

        var assignments = parseItems(Assignment.class, cur, ctx);
        if (assignments.isError()) {
            return error(assignments);
        }
        return ok(MergeUpdateAction.of(assignments.value()));
    }
}
