package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.lang.Tuple;
import io.cherlabs.sqm.lang.Tuple3;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.Token;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.repos.ParsersRepository;

import java.util.*;

/**
 * Full SELECT query parser.
 */
public final class QueryParser implements Parser<Query> {

    private static final Set<TokenType> SELECT_TERMINATORS = EnumSet.of(
        TokenType.FROM, TokenType.WHERE, TokenType.GROUP, TokenType.HAVING,
        TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    private static final Set<TokenType> FROM_OR_JOIN_TERMINATORS = EnumSet.of(
        TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS,
        TokenType.WHERE, TokenType.GROUP, TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    private static final Set<TokenType> WHERE_TERMINATORS = EnumSet.of(
        TokenType.GROUP, TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    private static final Set<TokenType> GROUP_TERMINATORS = EnumSet.of(
        TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    private static final Set<TokenType> HAVING_TERMINATORS = EnumSet.of(
        TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    private static final Set<TokenType> ORDER_TERMINATORS = EnumSet.of(
        TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    private static final Set<TokenType> ITEM_TERMINATORS = EnumSet.of(
        TokenType.COMMA, TokenType.EOF
    );

    private static final Set<TokenType> COMPOSITE_QUERY_INDICATORS = EnumSet.of(
        TokenType.UNION, TokenType.INTERSECT, TokenType.EXCEPT
    );

    private final ParsersRepository repository;

    public QueryParser() {
        this(Parsers.defaultRepository());
    }

    public QueryParser(ParsersRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Class<Query> targetType() {
        return Query.class;
    }

    @Override
    public ParseResult<Query> parse(Cursor cur) {
        if (looksLikeWith(cur)) {
            var wr = parseWith(cur);
            return finalize(cur, wr);
        }

        if (looksLikeComposite(cur)) {
            var cr = parseCompose(cur);
            return finalize(cur, cr);
        }

        var sr = parseSelect(cur);
        return finalize(cur, sr);
    }

    private ParseResult<Query> finalize(Cursor cur, ParseResult<? extends Query> pr) {
        if (pr.isError()) {
            return ParseResult.error(pr);
        }
        if (!cur.isEof()) {
            return ParseResult.error("Unexpected tokens at the end of query", cur.pos());
        }
        return ParseResult.ok(pr.value());
    }

    private boolean looksLikeWith(Cursor cur) {
        return cur.match(TokenType.WITH);
    }

    private boolean looksLikeComposite(Cursor cur) {
        return cur.find(COMPOSITE_QUERY_INDICATORS) < cur.size();
    }

    private ParseResult<SelectQuery> parseSelect(Cursor cur) {
        // SELECT
        if (!cur.consumeIf(TokenType.SELECT)) {
            return ParseResult.error("Expected SELECT at the beginning of a query", cur.pos());
        }

        var q = new SelectQuery();

        // TOP
        if (cur.consumeIf(TokenType.TOP)) { // T-SQL
            cur.consumeIf(TokenType.LPAREN); // skip '(' if number is wrapped.
            var t = cur.expect(TokenType.NUMBER);
            var top = Long.parseLong(t.lexeme());
            q.limit(top);
            cur.consumeIf(TokenType.RPAREN);
        }

        // DISTINCT
        if (cur.consumeIf(TokenType.DISTINCT)) {
            q.distinct(true);
        }

        // SELECT list
        var colParser = repository.require(Column.class);
        var selCur = cur.advance(cur.find(SELECT_TERMINATORS));
        while (!selCur.isEof()) {
            Cursor itemCur = selCur.advance(selCur.find(ITEM_TERMINATORS));
            var cr = colParser.parse(itemCur);
            if (!cr.ok()) return ParseResult.error(cr);
            q.select(cr.value());
            selCur.consumeIf(TokenType.COMMA); // skip comma if present
        }

        // FROM (optional)
        if (cur.consumeIf(TokenType.FROM)) {
            var tableParser = repository.require(Table.class);
            var fromCur = cur.advance(cur.find(FROM_OR_JOIN_TERMINATORS));
            var tr = tableParser.parse(fromCur);
            if (tr.isError()) return ParseResult.error(tr);
            q.from(tr.value());

            // JOINs (0..n)
            var joinParser = repository.require(Join.class);
            while (cur.matchAny(TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS)) {
                int i = 0;
                // calc how many tokens need to be skipped.
                while (cur.matchAny(i, TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS)) {
                    i++;
                }
                var joinCur = cur.advance(cur.find(FROM_OR_JOIN_TERMINATORS, i));
                var jr = joinParser.parse(joinCur);
                if (jr.isError()) return ParseResult.error(jr);
                q.join(jr.value());
            }
        }

        // WHERE (optional)
        if (cur.consumeIf(TokenType.WHERE)) {
            var whereCur = cur.advance(cur.find(WHERE_TERMINATORS));
            var fr = repository.require(Filter.class).parse(whereCur);
            if (fr.isError()) return ParseResult.error(fr);
            q.where(fr.value());
        }

        // GROUP BY (optional)
        if (cur.consumeIf(TokenType.GROUP)) {
            // expect IDENT "BY"
            cur.expect("Expected BY after GROUP", TokenType.BY);

            var groupCur = cur.advance(cur.find(GROUP_TERMINATORS));
            var grpParser = repository.require(Group.class);
            while (!groupCur.isEof()) {
                Cursor itemCur = groupCur.advance(groupCur.find(ITEM_TERMINATORS));
                var gr = grpParser.parse(itemCur);
                if (gr.isError()) return ParseResult.error(gr);
                q.groupBy().add(gr.value());
                groupCur.consumeIf(TokenType.COMMA);
            }
        }

        // HAVING (optional)
        if (cur.consumeIf(TokenType.HAVING)) {
            var havingCur = cur.advance(cur.find(HAVING_TERMINATORS));
            var hr = repository.require(Filter.class).parse(havingCur);
            if (hr.isError()) return ParseResult.error(hr);
            q.having(hr.value());
        }

        // ORDER BY, LIMIT & OFFSET (optional)
        var res = parseOrderByLimitAndOffset(cur);
        if (res.isError()) {
            return ParseResult.error(res.errorMessage(), res.problems().get(0).pos());
        }

        q.orderBy(res.value().first());
        var limit = res.value().second();
        if (limit != null) {
            q.limit(limit);
        }
        var offset = res.value().third();
        if (offset != null) {
            q.offset(offset);
        }

        return ParseResult.ok(q);
    }

    private ParseResult<CteQuery> parseCte(Cursor cur) {
        var name = cur.expect("Expected CTE name", TokenType.IDENT).lexeme();
        var aliases = new ArrayList<String>();

        if (cur.consumeIf(TokenType.LPAREN)) {
            do {
                aliases.add(cur.expect("Expected column name", TokenType.IDENT).lexeme());
            } while (cur.consumeIf(TokenType.COMMA));
            cur.expect("Expected ')'", TokenType.RPAREN);
        }

        cur.expect("Expected AS", TokenType.AS);
        cur.expect("Expected '(' before CTE subquery", TokenType.LPAREN);

        var subCur = cur.advance(cur.find(TokenType.RPAREN));
        var body = parse(subCur);
        if (body.isError()) {
            return ParseResult.error(body);
        }
        cur.expect("Expected '(' before CTE subquery", TokenType.RPAREN);
        return ParseResult.ok(new CteQuery(name, body.value(), aliases));
    }

    private ParseResult<WithQuery> parseWith(Cursor cur) {
        cur.expect(TokenType.WITH);

        final boolean recursive = cur.consumeIf(TokenType.RECURSIVE);

        List<CteQuery> ctes = new ArrayList<>();
        do {
            var cte = parseCte(cur);
            if (cte.isError()) {
                return ParseResult.error(cte);
            }
            ctes.add(cte.value());
        } while (cur.consumeIf(TokenType.COMMA));

        var body = parse(cur);
        if (body.isError()) {
            return ParseResult.error(body);
        }
        return ParseResult.ok(new WithQuery(body.value(), ctes, recursive));
    }

    private ParseResult<CompositeQuery> parseCompose(Cursor cur) {
        List<Query> terms = new ArrayList<>();
        List<CompositeQuery.Op> ops = new ArrayList<>();

        Token t;
        do {
            cur.consumeIf(TokenType.LPAREN); // remove '(' if presented.
            var subCur = cur.advance(cur.find(TokenType.UNION, TokenType.INTERSECT, TokenType.EXCEPT, TokenType.EOF));
            var term = parse(subCur);
            if (term.isError()) {
                return ParseResult.error(term);
            }
            cur.consumeIf(TokenType.RPAREN); // remove ')' if presented.
            terms.add(term.value());
            t = cur.isEof() ? cur.peek() : cur.advance();
            var isAll = cur.consumeIf(TokenType.ALL);
            switch (t.type()) {
                case UNION -> {
                    if (isAll)
                        ops.add(CompositeQuery.Op.unionAll());
                    else
                        ops.add(CompositeQuery.Op.union());
                }
                case INTERSECT -> {
                    if (isAll)
                        ops.add(CompositeQuery.Op.intersectAll());
                    else
                        ops.add(CompositeQuery.Op.intersect());
                }
                case EXCEPT -> {
                    if (isAll)
                        ops.add(CompositeQuery.Op.exceptAll());
                    else
                        ops.add(CompositeQuery.Op.except());
                }
            }
        } while (COMPOSITE_QUERY_INDICATORS.contains(t.type()));

        // ORDER BY, LIMIT & OFFSET (optional)
        var res = parseOrderByLimitAndOffset(cur);
        if (res.isError()) {
            return ParseResult.error(res.errorMessage(), res.problems().get(0).pos());
        }

        return ParseResult.ok(new CompositeQuery(terms, ops, res.value().first(), res.value().second(), res.value().third()));
    }

    private ParseResult<Long> parseOptionalFetchClause(Cursor cur) {
        // FETCH requires FIRST or NEXT
        if (!(cur.consumeIf(TokenType.FIRST) || cur.consumeIf(TokenType.NEXT))) {
            return ParseResult.error("Expected FIRST or NEXT after FETCH", cur.pos());
        }

        if (!cur.match(TokenType.NUMBER)) {
            return ParseResult.error("Expected number after FETCH FIRST/NEXT", cur.pos());
        }
        var t = cur.advance();
        var limit = (long) Double.parseDouble(t.lexeme());

        // Optional ROW / ROWS
        if (cur.match(TokenType.ROW) || cur.match(TokenType.ROWS)) {
            cur.advance();
        }

        if (!cur.consumeIf(TokenType.ONLY)) {
            return ParseResult.error("Expected ONLY at the end of FETCH clause", cur.pos());
        }
        return ParseResult.ok(limit);
    }

    private ParseResult<Tuple3<List<Order>, Long, Long>> parseOrderByLimitAndOffset(Cursor cur) {
        // ORDER BY (optional)
        List<Order> orderBy = new ArrayList<>();
        if (cur.consumeIf(TokenType.ORDER)) {
            cur.expect("Expected BY after ORDER", TokenType.BY);

            var orderCur = cur.advance(cur.find(ORDER_TERMINATORS));
            var orderParser = repository.require(Order.class);
            while (!orderCur.isEof()) {
                Cursor itemCur = orderCur.advance(orderCur.find(ITEM_TERMINATORS));
                var or = orderParser.parse(itemCur);
                if (or.isError()) return ParseResult.error(or);
                orderBy.add(or.value());
                orderCur.consumeIf(TokenType.COMMA);
            }
        }

        // LIMIT (optional) — numeric only
        Long limit = null;
        if (cur.consumeIf(TokenType.LIMIT)) {
            if (!cur.match(TokenType.NUMBER)) {
                return ParseResult.error("Expected number after LIMIT", cur.pos());
            }
            var t = cur.advance();
            limit = (long) Double.parseDouble(t.lexeme());
        }

        // ANSI FETCH (can appear with or without OFFSET)
        if (cur.consumeIf(TokenType.FETCH)) {
            var fr = parseOptionalFetchClause(cur);
            if (fr.isError()) {
                return ParseResult.error(fr.errorMessage(), fr.problems().get(0).pos());
            }
            limit = fr.value();
        }

        // OFFSET (optional) — numeric only
        Long offset = null;
        if (cur.consumeIf(TokenType.OFFSET)) {
            if (!cur.match(TokenType.NUMBER)) {
                return ParseResult.error("Expected number after OFFSET", cur.pos());
            }
            var t = cur.advance();
            offset = (long) Double.parseDouble(t.lexeme());

            // Optional ROW / ROWS
            if (cur.match(TokenType.ROW) || cur.match(TokenType.ROWS)) {
                cur.advance();
            }

            // Optional ANSI FETCH after OFFSET
            if (cur.consumeIf(TokenType.FETCH)) {
                var fr = parseOptionalFetchClause(cur);
                if (fr.isError()) {
                    return ParseResult.error(fr.errorMessage(), fr.problems().get(0).pos());
                }
                limit = fr.value();
            }
        }

        return ParseResult.ok(Tuple.of(orderBy, limit, offset));
    }
}
