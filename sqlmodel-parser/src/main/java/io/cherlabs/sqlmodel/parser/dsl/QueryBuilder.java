package io.cherlabs.sqlmodel.parser.dsl;

import io.cherlabs.sqlmodel.core.*;
import io.cherlabs.sqlmodel.parser.SpecParsers;
import io.cherlabs.sqlmodel.parser.repos.SpecParsersRepository;

import java.util.List;

public final class QueryBuilder {

    private final Query query;
    private final SpecParsersRepository parsers;

    private QueryBuilder(SpecParsersRepository parsers) {
        this.query = new Query();
        this.parsers = parsers;
    }

    public static QueryBuilder newBuilder() {
        return newBuilder(SpecParsers.defaultRepository());
    }

    public static QueryBuilder newBuilder(SpecParsersRepository parsersRepository) {
        return new QueryBuilder(parsersRepository);
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

    public QueryBuilder join(String joinSpec) {
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
        add(GroupItem.class, this.query.groupBy(), itemSpecs);
        return this;
    }

    public QueryBuilder orderBy(String... itemSpecs) {
        add(OrderItem.class, this.query.orderBy(), itemSpecs);
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
}
