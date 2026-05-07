package io.sqm.parser.ansi;

import io.sqm.core.Identifier;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.RowExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Baseline ANSI parser for {@link MergeInsertAction}.
 */
public class MergeInsertActionParser implements Parser<MergeInsertAction> {

    /**
     * Creates a merge-insert-action parser.
     */
    public MergeInsertActionParser() {
    }

    @Override
    public ParseResult<? extends MergeInsertAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected INSERT", TokenType.INSERT);
        return ParseResult.error("MERGE INSERT actions are not supported by this dialect", cur.fullPos());
    }

    /**
     * Parses the shared MERGE insert-action subset used by dialect-specific implementations.
     *
     * @param cur parser cursor positioned at {@code INSERT}
     * @param ctx parse context
     * @return parsed merge insert action or an error result
     */
    protected final ParseResult<? extends MergeInsertAction> parseSupportedAction(Cursor cur, ParseContext ctx) {
        cur.expect("Expected INSERT", TokenType.INSERT);

        List<Identifier> columns = List.of();
        if (cur.consumeIf(TokenType.LPAREN)) {
            columns = parseIdentifierItems(cur, "Expected MERGE INSERT target column");
            cur.expect("Expected ) after MERGE INSERT target columns", TokenType.RPAREN);
        }

        cur.expect("Expected VALUES after MERGE INSERT columns", TokenType.VALUES);
        var row = ctx.parse(RowExpr.class, cur);
        if (row.isError()) {
            return error(row);
        }

        return ok(MergeInsertAction.of(columns, row.value()));
    }

    @Override
    public Class<MergeInsertAction> targetType() {
        return MergeInsertAction.class;
    }
}
