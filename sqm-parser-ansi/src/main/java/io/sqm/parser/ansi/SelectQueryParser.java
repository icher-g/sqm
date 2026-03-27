package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SELECT query statements.
 */
@SuppressWarnings("unused")
public class SelectQueryParser implements Parser<SelectQuery> {
    /**
     * Creates a select-query parser.
     */
    public SelectQueryParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<SelectQuery> parse(Cursor cur, ParseContext ctx) {
        var q = SelectQuery.builder();

        var beforeSelect = parseBeforeSelectKeyword(cur, ctx, q);
        if (beforeSelect.isError()) {
            return error(beforeSelect);
        }

        cur.expect("Expected SELECT at the beginning of a query", TokenType.SELECT);

        var afterSelect = parseAfterSelectKeyword(cur, ctx, q);
        if (afterSelect.isError()) {
            return error(afterSelect);
        }

        var distinct = parseDistinctClause(cur, ctx, q);
        if (distinct.isError()) {
            return error(distinct);
        }

        var afterDistinct = parseAfterDistinctClause(cur, ctx, q);
        if (afterDistinct.isError()) {
            return error(afterDistinct);
        }

        var items = parseSelectItemsClause(cur, ctx, q);
        if (items.isError()) {
            return error(items);
        }

        var from = parseFromClause(cur, ctx, q);
        if (from.isError()) {
            return error(from);
        }

        var where = parseWhereClause(cur, ctx, q);
        if (where.isError()) {
            return error(where);
        }

        var groupBy = parseGroupByClause(cur, ctx, q);
        if (groupBy.isError()) {
            return error(groupBy);
        }

        var having = parseHavingClause(cur, ctx, q);
        if (having.isError()) {
            return error(having);
        }

        var window = parseWindowClause(cur, ctx, q);
        if (window.isError()) {
            return error(window);
        }

        var orderBy = parseOrderByClause(cur, ctx, q);
        if (orderBy.isError()) {
            return error(orderBy);
        }

        var pagination = parsePaginationClause(cur, ctx, q);
        if (pagination.isError()) {
            return error(pagination);
        }

        var afterPaginationValidation = validateAfterPagination(cur, ctx, q);
        if (afterPaginationValidation.isError()) {
            return error(afterPaginationValidation);
        }

        var locking = parseLockingClause(cur, ctx, q);
        if (locking.isError()) {
            return error(locking);
        }

        var completedValidation = validateCompletedQuery(cur, ctx, q);
        if (completedValidation.isError()) {
            return error(completedValidation);
        }

        return ok(q.build());
    }

    /**
     * Hook for dialect-specific tokens that may appear before {@code SELECT}.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseBeforeSelectKeyword(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        return ok(null);
    }

    /**
     * Hook for dialect-specific tokens that may appear after {@code SELECT}
     * and before DISTINCT / projection list.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseAfterSelectKeyword(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        return ok(null);
    }

    /**
     * Parses the DISTINCT clause.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseDistinctClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        if (!cur.match(TokenType.DISTINCT)) {
            return ok(null);
        }

        var distinct = ctx.parse(DistinctSpec.class, cur);
        if (distinct.isError()) {
            return error(distinct);
        }
        q.distinct(distinct.value());
        return ok(null);
    }

    /**
     * Hook for dialect-specific tokens that may appear after DISTINCT and before the projection list.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseAfterDistinctClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        return ok(null);
    }

    /**
     * Parses the projection list.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseSelectItemsClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        var items = parseItems(SelectItem.class, cur, ctx);
        if (items.isError()) {
            return error(items);
        }
        q.select(items.value());
        return ok(null);
    }

    /**
     * Parses the {@code FROM} clause and attached joins.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseFromClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        if (!cur.consumeIf(TokenType.FROM)) {
            return ok(null);
        }

        var tableRef = ctx.parse(TableRef.class, cur);
        if (tableRef.isError()) {
            return error(tableRef);
        }
        q.from(tableRef.value());

        while (cur.consumeIf(TokenType.COMMA)) {
            var crossJoin = ctx.parse(TableRef.class, cur);
            if (crossJoin.isError()) {
                return error(crossJoin);
            }
            q.join(CrossJoin.of(crossJoin.value()));
        }

        while (isJoinStart(cur)) {
            var join = ctx.parse(Join.class, cur);
            if (join.isError()) {
                return error(join);
            }
            q.join(join.value());
        }

        return ok(null);
    }

    /**
     * Checks whether the current cursor position starts a join clause.
     *
     * @param cur token cursor.
     * @return {@code true} when the cursor is positioned at a join-start token.
     */
    protected boolean isJoinStart(Cursor cur) {
        return cur.matchAny(Indicators.JOIN);
    }

    /**
     * Parses the {@code WHERE} clause.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseWhereClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        if (!cur.consumeIf(TokenType.WHERE)) {
            return ok(null);
        }

        var where = ctx.parse(Predicate.class, cur);
        if (where.isError()) {
            return error(where);
        }
        q.where(where.value());
        return ok(null);
    }

    /**
     * Parses the {@code GROUP BY} clause.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseGroupByClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        if (!(cur.match(TokenType.GROUP) && cur.match(TokenType.BY, 1))) {
            return ok(null);
        }

        var groupBy = ctx.parse(GroupBy.class, cur);
        if (groupBy.isError()) {
            return error(groupBy);
        }
        q.groupBy(groupBy.value().items());
        return ok(null);
    }

    /**
     * Parses the {@code HAVING} clause.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseHavingClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        if (!cur.consumeIf(TokenType.HAVING)) {
            return ok(null);
        }

        var having = ctx.parse(Predicate.class, cur);
        if (having.isError()) {
            return error(having);
        }
        q.having(having.value());
        return ok(null);
    }

    /**
     * Parses the {@code WINDOW} clause.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseWindowClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        while (cur.consumeIf(TokenType.WINDOW)) {
            do {
                var window = ctx.parse(WindowDef.class, cur);
                if (window.isError()) {
                    return error(window);
                }
                q.window(window.value());
            }
            while (cur.consumeIf(TokenType.COMMA));
        }
        return ok(null);
    }

    /**
     * Parses the {@code ORDER BY} clause.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseOrderByClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        if (!(cur.match(TokenType.ORDER) && cur.match(TokenType.BY, 1))) {
            return ok(null);
        }

        var orderBy = ctx.parse(OrderBy.class, cur);
        if (orderBy.isError()) {
            return error(orderBy);
        }
        q.orderBy(orderBy.value().items());
        return ok(null);
    }

    /**
     * Parses trailing pagination clauses such as LIMIT/OFFSET.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parsePaginationClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        var limitOffset = ctx.parse(LimitOffset.class, cur);
        if (limitOffset.isError()) {
            return error(limitOffset);
        }

        var value = limitOffset.value();
        if (value.limit() != null || value.offset() != null || value.limitAll()) {
            q.limitOffset(value);
        }
        return ok(null);
    }

    /**
     * Validates state after pagination parsing completed.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> validateAfterPagination(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        return ok(null);
    }

    /**
     * Parses the locking clause.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> parseLockingClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        if (!cur.match(TokenType.FOR)) {
            return ok(null);
        }

        var locking = ctx.parse(LockingClause.class, cur);
        if (locking.isError()) {
            return error(locking);
        }
        q.lockFor(locking.value());
        return ok(null);
    }

    /**
     * Validates the completed query before it is built.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    protected ParseResult<Void> validateCompletedQuery(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        return ok(null);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<SelectQuery> targetType() {
        return SelectQuery.class;
    }
}
