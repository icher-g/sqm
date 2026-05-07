package io.sqm.parser.ansi;

import io.sqm.core.Assignment;
import io.sqm.core.MergeUpdateAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Baseline ANSI parser for {@link MergeUpdateAction}.
 */
public class MergeUpdateActionParser implements Parser<MergeUpdateAction> {

    /**
     * Creates a merge-update-action parser.
     */
    public MergeUpdateActionParser() {
    }

    @Override
    public ParseResult<? extends MergeUpdateAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected UPDATE", TokenType.UPDATE);
        return ParseResult.error("MERGE UPDATE actions are not supported by this dialect", cur.fullPos());
    }

    /**
     * Parses the shared MERGE update-action subset used by dialect-specific implementations.
     *
     * @param cur parser cursor positioned at {@code UPDATE}
     * @param ctx parse context
     * @return parsed merge update action or an error result
     */
    protected final ParseResult<? extends MergeUpdateAction> parseSupportedAction(Cursor cur, ParseContext ctx) {
        cur.expect("Expected UPDATE", TokenType.UPDATE);
        cur.expect("Expected SET after MERGE UPDATE action", TokenType.SET);

        var assignments = parseItems(Assignment.class, cur, ctx);
        if (assignments.isError()) {
            return error(assignments);
        }
        return ok(MergeUpdateAction.of(assignments.value()));
    }

    @Override
    public Class<MergeUpdateAction> targetType() {
        return MergeUpdateAction.class;
    }
}
