package io.sqm.core.transform;

import io.sqm.core.Identifier;

import java.util.Objects;

/**
 * Qualification outcome for a table name.
 */
public sealed interface TableQualification
    permits TableQualification.Qualified, TableQualification.Unresolved, TableQualification.Ambiguous {
    /**
     * Returns outcome when table could not be resolved to a specific schema.
     *
     * @return unresolved outcome.
     */
    static TableQualification unresolved() {
        return Unresolved.INSTANCE;
    }

    /**
     * Returns outcome when table resolved to an explicit schema identifier.
     *
     * @param schema resolved schema identifier.
     * @return qualified outcome.
     */
    static TableQualification qualified(Identifier schema) {
        return new Qualified(Objects.requireNonNull(schema, "schema"));
    }

    /**
     * Returns outcome when table name is ambiguous across schemas.
     *
     * @return ambiguous outcome.
     */
    static TableQualification ambiguous() {
        return Ambiguous.INSTANCE;
    }

    /**
     * Resolved, schema-qualified table outcome.
     *
     * @param schema schema identifier.
     */
    record Qualified(Identifier schema) implements TableQualification {
    }

    /**
     * Unresolved table outcome.
     */
    final class Unresolved implements TableQualification {
        private static final Unresolved INSTANCE = new Unresolved();

        private Unresolved() {
        }
    }

    /**
     * Ambiguous table outcome.
     */
    final class Ambiguous implements TableQualification {
        private static final Ambiguous INSTANCE = new Ambiguous();

        private Ambiguous() {
        }
    }
}
