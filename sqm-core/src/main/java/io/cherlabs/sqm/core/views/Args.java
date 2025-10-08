package io.cherlabs.sqm.core.views;

import io.cherlabs.sqm.core.FunctionColumn;

import java.util.Optional;

/**
 * A view that provides access to {@link FunctionColumn.Arg} properties.
 */
public final class Args {
    private Args() {
    }

    /**
     * Gets a column name from the {@link FunctionColumn.Arg.Column} class.
     *
     * @param g the argument.
     * @return {@link Optional} with the name of the column or an {@link Optional#empty()}.
     */
    public static Optional<String> columnName(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Column h) return Optional.ofNullable(h.name());
        return Optional.empty();
    }

    /**
     * Gets a column table from the {@link FunctionColumn.Arg.Column} class.
     *
     * @param g the argument.
     * @return {@link Optional} with the name of the table or an {@link Optional#empty()}.
     */
    public static Optional<String> columnTable(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Column h) return Optional.ofNullable(h.table());
        return Optional.empty();
    }

    /**
     * Gets a value from the {@link FunctionColumn.Arg.Literal} class.
     *
     * @param g the argument.
     * @return {@link Optional} with the value or an {@link Optional#empty()}.
     */
    public static Optional<Object> literal(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Literal h) return Optional.ofNullable(h.value());
        return Optional.empty();
    }

    /**
     * Gets a * value from the {@link FunctionColumn.Arg.Star} class.
     *
     * @param g the argument.
     * @return {@link Optional} with the * value or an {@link Optional#empty()}.
     */
    public static Optional<String> star(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Star) return Optional.of("*");
        return Optional.empty();
    }

    /**
     * Gets a nested function from the {@link FunctionColumn.Arg.Function} class.
     *
     * @param g the argument.
     * @return {@link Optional} with the nested function or an {@link Optional#empty()}.
     */
    public static Optional<FunctionColumn> nestedFunction(FunctionColumn.Arg g) {
        if (g instanceof FunctionColumn.Arg.Function h) return Optional.ofNullable(h.call());
        return Optional.empty();
    }
}
