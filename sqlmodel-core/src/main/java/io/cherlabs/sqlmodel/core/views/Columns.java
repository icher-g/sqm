package io.cherlabs.sqlmodel.core.views;

import io.cherlabs.sqlmodel.core.*;
import io.cherlabs.sqlmodel.core.traits.*;

import java.util.List;
import java.util.Optional;

public final class Columns {
    private Columns() {
    }

    public static Optional<String> name(Column c) {
        if (c instanceof HasName h) return Optional.ofNullable(h.name());
        return Optional.empty();
    }

    public static Optional<String> alias(Column c) {
        if (c instanceof HasAlias h) return Optional.ofNullable(h.alias());
        return Optional.empty();
    }

    public static Optional<String> table(Column c) {
        if (c instanceof HasTableName h) return Optional.ofNullable(h.table());
        return Optional.empty();
    }

    public static Optional<String> expr(Column c) {
        if (c instanceof HasExpr h) return Optional.ofNullable(h.expression());
        return Optional.empty();
    }

    public static Optional<Query> query(Column c) {
        if (c instanceof HasQuery h) return Optional.ofNullable(h.query());
        return Optional.empty();
    }

    public static Optional<List<FunctionColumn.Arg>> functionArgs(Column c) {
        if (c instanceof HasArgs h) return Optional.ofNullable(h.args());
        return Optional.empty();
    }

    public static Optional<Boolean> distinct(Column c) {
        if (c instanceof HasDistinct h) return Optional.of(h.distinct());
        return Optional.empty();
    }

    public static Optional<List<WhenThen>> whens(Column c) {
        if (c instanceof HasWhens h) return Optional.of(h.whens());
        return Optional.empty();
    }

    public static Optional<Entity> elseValue(Column c) {
        if (c instanceof HasElseValue h) return Optional.of(h.elseValue());
        return Optional.empty();
    }
}
