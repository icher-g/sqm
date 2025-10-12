package io.cherlabs.sqm.core;

import io.cherlabs.sqm.core.traits.HasAlias;
import io.cherlabs.sqm.core.traits.HasArgs;
import io.cherlabs.sqm.core.traits.HasDistinct;
import io.cherlabs.sqm.core.traits.HasName;

import java.util.List;
import java.util.Objects;

/**
 * A scalar function used as a column, e.g. COUNT(DISTINCT t.id) AS c.
 */
public record FunctionColumn(
        String name,                 // bare or qualified; e.g., "lower" or "pg_catalog.lower"
        List<Arg> args,              // structured arguments
        Boolean distinct,            // COUNT(DISTINCT ...)
        String alias                 // optional
) implements Column, HasName, HasArgs, HasDistinct, HasAlias {

    public FunctionColumn {
        Objects.requireNonNull(name, "name");
    }

    /**
     * Creates a function column.
     *
     * @param name a function name
     * @param args an array of function arguments.
     * @return A newly created instance of a function column.
     */
    public static FunctionColumn of(String name, Arg... args) {
        return new FunctionColumn(name, List.of(args), false, null);
    }

    /**
     * Adds alias to a function column.
     *
     * @param alias an alias.
     * @return A new instance of the function column with the preserved other fields and new alias.
     */
    public FunctionColumn as(String alias) {
        return new FunctionColumn(name, args, distinct, alias);
    }

    /* -------- Function arguments (structured) -------- */
    public sealed interface Arg extends Entity permits Arg.Column, Arg.Literal, Arg.Function, Arg.Star {
        /**
         * Creates a column argument.
         *
         * @param name a name of the column.
         * @return A newly created instance of the column argument.
         */
        static Arg column(String name) {
            return new Column(null, name);
        }

        /**
         * Creates a column argument.
         *
         * @param table a table name.
         * @param name  a column name.
         * @return A newly created instance of the column argument.
         */
        static Arg column(String table, String name) {
            return new Column(table, name);
        }

        /**
         * Creates a literal argument.
         *
         * @param value a literal
         * @return A newly created instance of the literal argument.
         */
        static Arg lit(Object value) {
            return new Literal(value);
        }

        /**
         * Creates a function argument. Used for nested functions.
         *
         * @param call a function column.
         * @return A newly created instance of the function column argument.
         */
        static Arg func(FunctionColumn call) {
            return new Function(call);
        }

        /**
         * Creates a start '*' argument.
         *
         * @return A newly created instance of a star argument.
         */
        static Arg star() {
            return new Star();
        }

        /**
         * Column reference argument: t.c or just c.
         */
        record Column(String table, String name) implements Arg {
        }

        /**
         * Literal argument (String, Number, Boolean, null).
         */
        record Literal(Object value) implements Arg {
        }

        /**
         * Nested function call as an argument.
         */
        record Function(FunctionColumn call) implements Arg {
        }

        /**
         * The '*' argument (e.g., COUNT(*))
         */
        record Star() implements Arg {
        }
    }
}
