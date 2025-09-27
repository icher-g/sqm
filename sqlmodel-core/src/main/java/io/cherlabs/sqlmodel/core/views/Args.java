package io.cherlabs.sqlmodel.core.views;

import io.cherlabs.sqlmodel.core.FunctionColumn;

import java.util.Optional;

public final class Args {
    private Args() {
    }

    public static Optional<String> columnName(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Column h) return Optional.ofNullable(h.name());
        return Optional.empty();
    }

    public static Optional<String> columnTable(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Column h) return Optional.ofNullable(h.table());
        return Optional.empty();
    }

    public static Optional<Object> literal(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Literal h) return Optional.ofNullable(h.value());
        return Optional.empty();
    }

    public static Optional<String> star(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Star h) return Optional.of("*");
        return Optional.empty();
    }

    public static Optional<FunctionColumn> nestedFunction(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Function h) return Optional.ofNullable(h.call());
        return Optional.empty();
    }
}
