package io.sqm.parser.mysql;

import io.sqm.core.SelectModifier;
import io.sqm.core.SelectQueryBuilder;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * MySQL SELECT parser with support for SELECT modifiers and optimizer hint comments.
 */
public class SelectQueryParser extends io.sqm.parser.ansi.SelectQueryParser {

    /**
     * Creates a MySQL SELECT parser.
     */
    public SelectQueryParser() {
    }

    /**
     * Parses tokens that may appear before {@code SELECT}.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   query builder.
     * @return parse result.
     */
    @Override
    protected ParseResult<Void> parseBeforeSelectKeyword(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        while (cur.match(TokenType.COMMENT_HINT)) {
            if (!ctx.capabilities().supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
                return error("Optimizer hint comments are not supported by this dialect", cur.fullPos());
            }
            q.optimizerHint(cur.advance().lexeme());
        }
        return ok(null);
    }

    /**
     * Parses tokens that may appear right after {@code SELECT} and before projection.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   query builder.
     * @return parse result.
     */
    @Override
    protected ParseResult<Void> parseAfterSelectKeyword(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        while (true) {
            if (cur.match(TokenType.COMMENT_HINT)) {
                if (!ctx.capabilities().supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
                    return error("Optimizer hint comments are not supported by this dialect", cur.fullPos());
                }
                q.optimizerHint(cur.advance().lexeme());
                continue;
            }
            if (cur.match(TokenType.IDENT) && "SQL_CALC_FOUND_ROWS".equalsIgnoreCase(cur.peek().lexeme())) {
                if (!ctx.capabilities().supports(SqlFeature.CALC_FOUND_ROWS_MODIFIER)) {
                    return error("SQL_CALC_FOUND_ROWS is not supported by this dialect", cur.fullPos());
                }
                cur.advance();
                q.selectModifier(SelectModifier.CALC_FOUND_ROWS);
                continue;
            }
            break;
        }
        return ok(null);
    }
}
