package io.sqm.parser.postgresql;

import io.sqm.core.Identifier;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.RowExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses PostgreSQL {@code MERGE ... WHEN NOT MATCHED THEN INSERT ... VALUES (...)} actions.
 */
public class MergeInsertActionParser extends io.sqm.parser.ansi.MergeInsertActionParser {

    /**
     * Creates a PostgreSQL merge-insert-action parser.
     */
    public MergeInsertActionParser() {
    }

    @Override
    public ParseResult<? extends MergeInsertAction> parse(Cursor cur, ParseContext ctx) {
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
}
