package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasArgs;
import io.cherlabs.sqlmodel.core.traits.HasDistinct;
import io.cherlabs.sqlmodel.core.traits.HasName;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A scalar function used as a column, e.g. COUNT(DISTINCT t.id) AS c.
 */
public record FunctionColumn(
        String name,                 // bare or qualified; e.g., "lower" or "pg_catalog.lower"
        List<Arg> args,              // structured arguments
        boolean distinct,            // COUNT(DISTINCT ...)
        String alias                 // optional
) implements Column, HasName, HasArgs, HasDistinct, HasAlias {

    public FunctionColumn {
        Objects.requireNonNull(name, "name");
    }

    public static FunctionColumn of(String name, Arg... args) {
        return new FunctionColumn(name, List.of(args), false, null);
    }

    public FunctionColumn as(String alias) {
        return new FunctionColumn(name, args, distinct, alias);
    }

    /* -------- Function arguments (structured) -------- */
    public sealed interface Arg extends Entity permits Arg.Column, Arg.Literal, Arg.Function, Arg.Star {
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

        static Arg column(String name) {
            return new Column(null, name);
        }

        static Arg column(String table, String name) {
            return new Column(table, name);
        }

        static Arg lit(Object value) {
            return new Literal(value);
        }

        static Arg func(FunctionColumn call) {
            return new Function(call);
        }

        static Arg star() {
            return new Star();
        }
    }
}
