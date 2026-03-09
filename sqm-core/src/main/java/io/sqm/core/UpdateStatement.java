package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models a dialect-neutral {@code UPDATE} statement.
 */
public non-sealed interface UpdateStatement extends Statement {

    /**
     * Creates an immutable update statement.
     *
     * @param table target table
     * @param assignments update assignments
     * @param from optional FROM sources
     * @param where optional predicate
     * @return immutable update statement
     */
    static UpdateStatement of(Table table, List<Assignment> assignments, List<TableRef> from, Predicate where) {
        return new Impl(table, assignments, from, where);
    }

    /**
     * Creates an immutable update statement.
     *
     * @param table target table
     * @param assignments update assignments
     * @param where optional predicate
     * @return immutable update statement
     */
    static UpdateStatement of(Table table, List<Assignment> assignments, Predicate where) {
        return new Impl(table, assignments, List.of(), where);
    }

    /**
     * Creates an immutable update statement without a {@code WHERE} clause.
     *
     * @param table target table
     * @param assignments update assignments
     * @return immutable update statement
     */
    static UpdateStatement of(Table table, List<Assignment> assignments) {
        return new Impl(table, assignments, List.of(), null);
    }

    /**
     * Creates a mutable builder for constructing immutable update statements.
     *
     * @param table target table
     * @return builder initialized with the target table
     */
    static Builder builder(Table table) {
        return new Builder(table);
    }

    /**
     * Returns the update target table.
     *
     * @return target table
     */
    Table table();

    /**
     * Returns the immutable list of assignments.
     *
     * @return update assignments
     */
    List<Assignment> assignments();

    /**
     * Returns optional {@code FROM} sources.
     *
     * @return immutable from source list
     */
    List<TableRef> from();

    /**
     * Returns the optional {@code WHERE} predicate.
     *
     * @return predicate or {@code null}
     */
    Predicate where();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitUpdateStatement(this);
    }

    /**
     * Mutable builder for constructing immutable {@link UpdateStatement} instances.
     */
    final class Builder {
        private Table table;
        private final List<Assignment> assignments = new ArrayList<>();
        private final List<TableRef> from = new ArrayList<>();
        private Predicate where;

        /**
         * Creates a builder initialized with a target table.
         *
         * @param table target table
         */
        private Builder(Table table) {
            this.table = Objects.requireNonNull(table, "table");
        }

        /**
         * Sets the target table.
         *
         * @param table target table
         * @return this builder
         */
        public Builder table(Table table) {
            this.table = Objects.requireNonNull(table, "table");
            return this;
        }

        /**
         * Replaces the full assignment list.
         *
         * @param assignments assignments to use
         * @return this builder
         */
        public Builder assignments(List<Assignment> assignments) {
            Objects.requireNonNull(assignments, "assignments");
            this.assignments.clear();
            this.assignments.addAll(assignments);
            return this;
        }

        /**
         * Replaces optional {@code FROM} sources.
         *
         * @param from from sources
         * @return this builder
         */
        public Builder from(List<TableRef> from) {
            Objects.requireNonNull(from, "from");
            this.from.clear();
            this.from.addAll(from);
            return this;
        }

        /**
         * Replaces optional {@code FROM} sources.
         *
         * @param from from sources
         * @return this builder
         */
        public Builder from(TableRef... from) {
            Objects.requireNonNull(from, "from");
            return from(List.of(from));
        }

        /**
         * Appends one assignment.
         *
         * @param assignment assignment to append
         * @return this builder
         */
        public Builder set(Assignment assignment) {
            this.assignments.add(Objects.requireNonNull(assignment, "assignment"));
            return this;
        }

        /**
         * Appends one assignment.
         *
         * @param column target column
         * @param value assigned expression
         * @return this builder
         */
        public Builder set(Identifier column, Expression value) {
            return set(Assignment.of(column, value));
        }

        /**
         * Sets the optional {@code WHERE} predicate.
         *
         * @param where predicate or {@code null}
         * @return this builder
         */
        public Builder where(Predicate where) {
            this.where = where;
            return this;
        }

        /**
         * Builds an immutable update statement.
         *
         * @return immutable update statement
         */
        public UpdateStatement build() {
            if (table == null) {
                throw new IllegalStateException("table must be set");
            }
            if (assignments.isEmpty()) {
                throw new IllegalStateException("at least one assignment is required");
            }
            return UpdateStatement.of(table, assignments, from, where);
        }
    }

    /**
     * Default immutable update statement implementation.
     *
     * @param table target table
     * @param assignments assignments
     * @param from optional from sources
     * @param where optional predicate
     */
    record Impl(Table table, List<Assignment> assignments, List<TableRef> from, Predicate where) implements UpdateStatement {
        /**
         * Creates an immutable update statement implementation.
         */
        public Impl {
            Objects.requireNonNull(table, "table");
            assignments = List.copyOf(Objects.requireNonNull(assignments, "assignments"));
            from = from == null ? List.of() : List.copyOf(from);
            if (assignments.isEmpty()) {
                throw new IllegalArgumentException("assignments must not be empty");
            }
        }
    }
}
