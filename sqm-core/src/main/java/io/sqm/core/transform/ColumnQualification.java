package io.sqm.core.transform;

import io.sqm.core.Identifier;

import java.util.Objects;

/**
 * Qualification outcome for an unqualified column reference.
 */
public sealed interface ColumnQualification
    permits ColumnQualification.Qualified, ColumnQualification.Unresolved, ColumnQualification.Ambiguous {

    /**
     * Returns a qualified column outcome using the provided table alias/name qualifier identifier.
     *
     * @param qualifier table alias or table name qualifier identifier
     * @return qualified outcome
     */
    static ColumnQualification qualified(Identifier qualifier) {
        return new Qualified(Objects.requireNonNull(qualifier, "qualifier"));
    }

    /**
     * Returns an unresolved column outcome.
     *
     * @return unresolved outcome
     */
    static ColumnQualification unresolved() {
        return Unresolved.INSTANCE;
    }

    /**
     * Returns an ambiguous column outcome.
     *
     * @return ambiguous outcome
     */
    static ColumnQualification ambiguous() {
        return Ambiguous.INSTANCE;
    }

    /**
     * Resolved qualification outcome.
     *
     * @param qualifier table alias or table name qualifier identifier
     */
    record Qualified(Identifier qualifier) implements ColumnQualification {
    }

    /**
     * Unresolved qualification outcome.
     */
    final class Unresolved implements ColumnQualification {
        private static final Unresolved INSTANCE = new Unresolved();

        private Unresolved() {
        }
    }

    /**
     * Ambiguous qualification outcome.
     */
    final class Ambiguous implements ColumnQualification {
        private static final Ambiguous INSTANCE = new Ambiguous();

        private Ambiguous() {
        }
    }
}
