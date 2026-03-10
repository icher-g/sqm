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
     * @param joins optional joined sources attached to the target table
     * @param from optional FROM sources
     * @param where optional predicate
     * @param returning optional returning projection items
     * @return immutable update statement
     */
    static UpdateStatement of(Table table,
                              List<Assignment> assignments,
                              List<Join> joins,
                              List<TableRef> from,
                              Predicate where,
                              List<SelectItem> returning) {
        return new Impl(table, assignments, joins, from, where, returning);
    }

    /**
     * Creates an immutable update statement.
     *
     * @param table target table
     * @param assignments update assignments
     * @param from optional FROM sources
     * @param where optional predicate
     * @param returning optional returning projection items
     * @return immutable update statement
     */
    static UpdateStatement of(Table table,
                              List<Assignment> assignments,
                              List<TableRef> from,
                              Predicate where,
                              List<SelectItem> returning) {
        return of(table, assignments, List.of(), from, where, returning);
    }

    /**
     * Creates an immutable update statement.
     *
     * @param table target table
     * @param assignments update assignments
     * @param joins optional joined sources attached to the target table
     * @param from optional FROM sources
     * @param where optional predicate
     * @return immutable update statement
     */
    static UpdateStatement of(Table table,
                              List<Assignment> assignments,
                              List<Join> joins,
                              List<TableRef> from,
                              Predicate where) {
        return of(table, assignments, joins, from, where, List.of());
    }

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
        return of(table, assignments, List.of(), from, where, List.of());
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
        return of(table, assignments, List.of(), List.of(), where, List.of());
    }

    /**
     * Creates an immutable update statement without a {@code WHERE} clause.
     *
     * @param table target table
     * @param assignments update assignments
     * @param joins optional joined sources attached to the target table
     * @return immutable update statement
     */
    static UpdateStatement of(Table table, List<Assignment> assignments, List<Join> joins) {
        return of(table, assignments, joins, List.of(), null, List.of());
    }

    /**
     * Creates an immutable update statement without a {@code WHERE} clause.
     *
     * @param table target table
     * @param assignments update assignments
     * @return immutable update statement
     */
    static UpdateStatement of(Table table, List<Assignment> assignments) {
        return of(table, assignments, List.of());
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
     * Returns optional joined sources attached to the target table.
     *
     * @return immutable join list
     */
    List<Join> joins();

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
     * Returns optional {@code RETURNING} projection items.
     *
     * @return immutable returning projection list
     */
    List<SelectItem> returning();

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
        private final List<Join> joins = new ArrayList<>();
        private final List<TableRef> from = new ArrayList<>();
        private Predicate where;
        private final List<SelectItem> returning = new ArrayList<>();

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
         * Replaces joined sources attached to the target table.
         *
         * @param joins joins to use
         * @return this builder
         */
        public Builder joins(List<Join> joins) {
            Objects.requireNonNull(joins, "joins");
            this.joins.clear();
            this.joins.addAll(joins);
            return this;
        }

        /**
         * Replaces joined sources attached to the target table.
         *
         * @param joins joins to use
         * @return this builder
         */
        public Builder joins(Join... joins) {
            Objects.requireNonNull(joins, "joins");
            return joins(List.of(joins));
        }

        /**
         * Appends one join attached to the target table.
         *
         * @param join join to append
         * @return this builder
         */
        public Builder join(Join join) {
            this.joins.add(Objects.requireNonNull(join, "join"));
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
         * Replaces optional {@code RETURNING} projection items.
         *
         * @param returning returning items
         * @return this builder
         */
        public Builder returning(List<SelectItem> returning) {
            Objects.requireNonNull(returning, "returning");
            this.returning.clear();
            this.returning.addAll(returning);
            return this;
        }

        /**
         * Replaces optional {@code RETURNING} projection items.
         *
         * @param returning returning items
         * @return this builder
         */
        public Builder returning(SelectItem... returning) {
            Objects.requireNonNull(returning, "returning");
            return returning(List.of(returning));
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
            return UpdateStatement.of(table, assignments, joins, from, where, returning);
        }
    }

    /**
     * Default immutable update statement implementation.
     *
     * @param table target table
     * @param assignments assignments
     * @param joins optional joined sources attached to the target table
     * @param from optional from sources
     * @param where optional predicate
     * @param returning optional returning projection items
     */
    record Impl(Table table,
                List<Assignment> assignments,
                List<Join> joins,
                List<TableRef> from,
                Predicate where,
                List<SelectItem> returning) implements UpdateStatement {
        /**
         * Creates an immutable update statement implementation.
         */
        public Impl {
            Objects.requireNonNull(table, "table");
            assignments = List.copyOf(Objects.requireNonNull(assignments, "assignments"));
            joins = joins == null ? List.of() : List.copyOf(joins);
            from = from == null ? List.of() : List.copyOf(from);
            returning = returning == null ? List.of() : List.copyOf(returning);
            if (assignments.isEmpty()) {
                throw new IllegalArgumentException("assignments must not be empty");
            }
        }
    }
}
