package io.sqm.parser.sqlserver;

import io.sqm.core.Expression;
import io.sqm.core.SelectQueryBuilder;
import io.sqm.core.TopSpec;
import io.sqm.parser.ansi.SelectQueryParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * SQL Server SELECT parser with support for DISTINCT + TOP ordering and
 * SQL Server pagination constraints.
 */
public class SqlServerSelectQueryParser extends SelectQueryParser {

    /**
     * Creates a SQL Server SELECT parser.
     */
    public SqlServerSelectQueryParser() {
    }

    /**
     * Parses SQL Server tokens that may appear after DISTINCT and before the projection list.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    @Override
    protected ParseResult<Void> parseAfterDistinctClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        var top = parseTopClause(cur, ctx);
        if (top.isError()) {
            return error(top);
        }

        if (top.value() != null) {
            q.top(top.value());
        }
        return ok(null);
    }

    /**
     * Validates SQL Server pagination rules that span multiple SELECT clauses.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    @Override
    protected ParseResult<Void> validateAfterPagination(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        var limitOffset = q.currentLimitOffset();
        if (limitOffset == null || (limitOffset.limit() == null && limitOffset.offset() == null && !limitOffset.limitAll())) {
            return ok(null);
        }

        if (q.currentTopSpec() != null) {
            return error("Cannot combine TOP with OFFSET/FETCH in SQL Server baseline support", cur.fullPos());
        }

        if (q.currentOrderBy() == null) {
            return error("OFFSET/FETCH requires ORDER BY in SQL Server", cur.fullPos());
        }

        return ok(null);
    }

    private ParseResult<TopSpec> parseTopClause(Cursor cur, ParseContext ctx) {
        if (!cur.consumeIf(TokenType.TOP)) {
            return ok(null);
        }

        var parenthesized = cur.consumeIf(TokenType.LPAREN);
        var expr = ctx.parse(Expression.class, cur);
        if (expr.isError()) {
            return error(expr);
        }
        var count = expr.value();
        if (parenthesized) {
            cur.expect("Expected ')' to close TOP expression", TokenType.RPAREN);
        }

        var percent = cur.consumeIf(TokenType.PERCENT);

        boolean withTies = false;
        if (cur.consumeIf(TokenType.WITH)) {
            cur.expect("Expected TIES after WITH in TOP clause", TokenType.TIES);
            withTies = true;
        }

        if (percent) {
            return error("TOP PERCENT is not supported by SQL Server baseline support", cur.fullPos());
        }

        if (withTies) {
            return error("TOP WITH TIES is not supported by SQL Server baseline support", cur.fullPos());
        }

        return ok(TopSpec.of(count, percent, withTies));
    }
}
