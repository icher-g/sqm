package io.cherlabs.sqlmodel.parser.dsl;

import io.cherlabs.sqlmodel.core.*;
import io.cherlabs.sqlmodel.parser.SpecParsers;
import io.cherlabs.sqlmodel.parser.repos.SpecParsersRepository;

import java.util.List;

public final class QueryBuilder {

    private final Query<?> query;
    private final SpecParsersRepository parsers;

    private QueryBuilder(Query<?> query, SpecParsersRepository parsers) {
        this.query = query == null ? new SelectQuery() : query;
        this.parsers = parsers;
    }

    public static QueryBuilder newBuilder() {
        return newBuilder(new SelectQuery(), SpecParsers.defaultRepository());
    }

    public static QueryBuilder newBuilder(Query<?> query) {
        return newBuilder(query, SpecParsers.defaultRepository());
    }

    public static QueryBuilder newBuilder(SpecParsersRepository parsersRepository) {
        return newBuilder(new SelectQuery(), parsersRepository);
    }

    public static QueryBuilder newBuilder(Query<?> query, SpecParsersRepository parsersRepository) {
        return new QueryBuilder(query, parsersRepository);
    }

    public QueryBuilder select(String... specs) {
        add(Column.class, this.query.select(), specs);
        return this;
    }

    public QueryBuilder from(String tableSpec) {
        var table = create(Table.class, tableSpec);
        query.from(table);
        return this;
    }

    public QueryBuilder innerJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Inner, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    public QueryBuilder leftJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Left, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    public QueryBuilder rightJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Right, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    public QueryBuilder fullJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Full, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    public QueryBuilder crossJoin(String joinSpec) {
        joinSpec = appendJoin(Join.JoinType.Cross, joinSpec);
        var join = create(Join.class, joinSpec);
        query.join(join);
        return this;
    }

    public QueryBuilder where(String whereSpec) {
        var filter = create(Filter.class, whereSpec);
        query.where(filter);
        return this;
    }

    public QueryBuilder having(String havingSpec) {
        var filter = create(Filter.class, havingSpec);
        query.having(filter);
        return this;
    }

    public QueryBuilder groupBy(String... itemSpecs) {
        add(Group.class, this.query.groupBy(), itemSpecs);
        return this;
    }

    public QueryBuilder orderBy(String... itemSpecs) {
        add(Order.class, this.query.orderBy(), itemSpecs);
        return this;
    }

    public QueryBuilder limit(int limit) {
        this.query.limit(limit);
        return this;
    }

    public QueryBuilder offset(int offset) {
        this.query.offset(offset);
        return this;
    }

    public Query build() {
        return this.query;
    }

    private <T extends Entity> void add(Class<T> type, List<T> list, String... specs) {
        var parser = parsers.require(type);
        for (String spec : specs) {
            var result = parser.parse(spec);
            if (!result.ok()) {
                throw new IllegalArgumentException(result.problems().toString());
            }
            list.add(result.value());
        }
    }

    private <T extends Entity> T create(Class<T> type, String spec) {
        var parser = parsers.require(type);
        var result = parser.parse(spec);
        if (!result.ok()) {
            throw new IllegalArgumentException(result.problems().toString());
        }
        return result.value();
    }

    private static String appendJoin(Join.JoinType joinType, String joinSpec) {
        var joinStr = joinType.toString().toUpperCase();
        var startsWith = joinSpec.regionMatches(true, 0, joinStr, 0, joinStr.length());
        if (!startsWith) {
            return joinStr + " JOIN " + joinSpec;
        }
        return joinSpec;
    }
}
