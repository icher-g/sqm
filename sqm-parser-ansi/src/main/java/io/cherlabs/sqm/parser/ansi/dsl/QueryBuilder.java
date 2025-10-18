package io.cherlabs.sqm.parser.ansi.dsl;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.parser.ansi.AnsiSpecs;
import io.cherlabs.sqm.parser.ansi.Parsers;
import io.cherlabs.sqm.parser.spi.ParseContext;

import java.util.ArrayList;
import java.util.List;

/**
 * This class helps to create SQL model from string expressions.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     QueryBuilder qb = QueryBuilder.newBuilder();
 *
 *     qb.select("u.user_name", "o.status", "count(*) AS cnt")
 *       .from("orders AS o")
 *       .where("o.status IN ('A','B')")
 *       .innerJoin("users AS u ON u.id = o.user_id")
 *       .groupBy("u.user_name, o.status")
 *       .having("count(*) > 10");
 *
 *     Query q = qb.build();
 *     }
 * </pre>
 */
public final class QueryBuilder {

    private final SelectQuery query;
    private final ParseContext ctx;

    private QueryBuilder(SelectQuery query, ParseContext ctx) {
        this.query = query == null ? new SelectQuery() : query;
        this.ctx = ctx;
    }

    /**
     * Creates new instance of {@link QueryBuilder} with default query: {@link SelectQuery} and specs repository: {@link Parsers#ansi()}l
     *
     * @return a new instance of {@link QueryBuilder}.
     */
    public static QueryBuilder newBuilder() {
        return newBuilder(new SelectQuery(), ParseContext.of(new AnsiSpecs()));
    }

    /**
     * Creates new instance of {@link QueryBuilder} with provided query and default specs repository: {@link Parsers#ansi()}.
     *
     * @param query a query instance to build.
     * @return a new instance of {@link QueryBuilder}.
     */
    public static QueryBuilder newBuilder(SelectQuery query) {
        return newBuilder(query, ParseContext.of(new AnsiSpecs()));
    }

    /**
     * Creates new instance of {@link QueryBuilder} with default query: {@link SelectQuery} and provided specs repository.
     *
     * @param ctx the parse context.
     * @return a new instance of {@link QueryBuilder}.
     */
    public static QueryBuilder newBuilder(ParseContext ctx) {
        return newBuilder(new SelectQuery(), ctx);
    }

    /**
     * Creates new instance of {@link QueryBuilder} with provided query and specs repository.
     *
     * @param query a query to build.
     * @param ctx   the parse context.
     * @return a new instance of {@link QueryBuilder}.
     */
    public static QueryBuilder newBuilder(SelectQuery query, ParseContext ctx) {
        return new QueryBuilder(query, ctx);
    }

    private static String appendJoin(Join.JoinType joinType, String joinSpec) {
        var joinStr = joinType.toString().toUpperCase();
        var startsWith = joinSpec.regionMatches(true, 0, joinStr, 0, joinStr.length());
        if (!startsWith) {
            return joinStr + " JOIN " + joinSpec;
        }
        return joinSpec;
    }

    /**
     * Adds columns to a query's select statement.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     select("u.user_name", "o.status", "count(*) AS cnt");
     *     }
     * </pre>
     *
     * @param specs a list of columns specifications.
     * @return this.
     */
    public QueryBuilder select(String... specs) {
        List<Column> columns = new ArrayList<>();
        for (String spec : specs) {
            var column = create(Column.class, spec);
            columns.add(column);
        }
        query.select(columns);
        return this;
    }

    /**
     * Adds from statement to a query from the table specification.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     from("orders AS o");
     *     }
     * </pre>
     *
     * @param tableSpec a table specification.
     * @return this.
     */
    public QueryBuilder from(String tableSpec) {
        var table = create(Table.class, tableSpec);
        query.from(table);
        return this;
    }

    /**
     * Adds inner join statement to a query.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     innerJoin("users AS u ON u.id = o.user_id");
     *     }
     * </pre>
     *
     * @param joinSpec a join specification.
     * @return this.
     */
    public QueryBuilder innerJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Inner, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    /**
     * Adds left join statement to a query.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     leftJoin("users AS u ON u.id = o.user_id");
     *     }
     * </pre>
     *
     * @param joinSpec a join specification.
     * @return this.
     */
    public QueryBuilder leftJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Left, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    /**
     * Adds right join statement to a query.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     rightJoin("users AS u ON u.id = o.user_id");
     *     }
     * </pre>
     *
     * @param joinSpec a join specification.
     * @return this.
     */
    public QueryBuilder rightJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Right, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    /**
     * Adds full join statement to a query.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     fullJoin("users AS u ON u.id = o.user_id");
     *     }
     * </pre>
     *
     * @param joinSpec a join specification.
     * @return this.
     */
    public QueryBuilder fullJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Full, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    /**
     * Adds cross join statement to a query.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     crossJoin("regions r");
     *     }
     * </pre>
     *
     * @param joinSpec a join specification.
     * @return this.
     */
    public QueryBuilder crossJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Cross, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    /**
     * Adds where statement to a query.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     where("o.status IN ('A','B')");
     *     }
     * </pre>
     *
     * @param whereSpec a where specification.
     * @return this.
     */
    public QueryBuilder where(String whereSpec) {
        var filter = create(Filter.class, whereSpec);
        query.where(filter);
        return this;
    }

    /**
     * Adds having statement to a query.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     having("count(*) > 10");
     *     }
     * </pre>
     *
     * @param havingSpec a having specification.
     * @return this.
     */
    public QueryBuilder having(String havingSpec) {
        var filter = create(Filter.class, havingSpec);
        query.having(filter);
        return this;
    }

    /**
     * Adds group by statement to a query.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     groupBy("u.user_name, o.status");
     *     }
     * </pre>
     *
     * @param itemSpecs a list of group by items specifications.
     * @return this.
     */
    public QueryBuilder groupBy(String... itemSpecs) {
        List<Group> items = new ArrayList<>();
        for (var spec : itemSpecs) {
            var item = create(Group.class, spec);
            items.add(item);
        }
        this.query.groupBy(items);
        return this;
    }

    /**
     * Adds an order by statement to a query.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     orderBy("u.user_name ASC, o.status DESC");
     *     }
     * </pre>
     *
     * @param itemSpecs a list of order by items specifications.
     * @return this.
     */
    public QueryBuilder orderBy(String... itemSpecs) {
        List<Order> items = new ArrayList<>();
        for (var spec : itemSpecs) {
            var item = create(Order.class, spec);
            items.add(item);
        }
        this.query.orderBy(items);
        return this;
    }

    /**
     * Adds a limit statement to a query.
     *
     * @param limit a limit.
     * @return this.
     */
    public QueryBuilder limit(long limit) {
        this.query.limit(limit);
        return this;
    }

    /**
     * Adds an offset statement to a query.
     *
     * @param offset an offset.
     * @return this.
     */
    public QueryBuilder offset(long offset) {
        this.query.offset(offset);
        return this;
    }

    /**
     * Builds a query model.
     *
     * @return a query.
     */
    public SelectQuery build() {
        return this.query;
    }

    private <T extends Entity> T create(Class<T> type, String spec) {
        var result = ctx.parse(type, spec);
        if (result.isError()) {
            throw new IllegalArgumentException(result.errorMessage());
        }
        return result.value();
    }
}
