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
     * @param output optional SQL Server output clause
     * @param returning optional returning projection items
     * @param optimizerHints optimizer hints (without comment delimiters)
     * @return immutable update statement
     */
    static UpdateStatement of(Table table,
                              List<Assignment> assignments,
                              List<Join> joins,
                              List<TableRef> from,
                              Predicate where,
                              OutputClause output,
                              List<SelectItem> returning,
                              List<String> optimizerHints) {
        return new Impl(table, assignments, joins, from, where, output, returning, optimizerHints);
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
     * Creates a mutable builder initialized from an existing update statement snapshot.
     *
     * @param statement source update statement
     * @return builder initialized with the statement state
     */
    static Builder builder(UpdateStatement statement) {
        return new Builder(statement);
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
     * Returns optional SQL Server {@code OUTPUT} clause.
     *
     * @return output clause or {@code null}
     */
    OutputClause output();

    /**
     * Returns optional {@code RETURNING} projection items.
     *
     * @return immutable returning projection list
     */
    List<SelectItem> returning();

    /**
     * Returns optimizer hints attached to this statement.
     *
     * @return immutable optimizer hint list
     */
    List<String> optimizerHints();

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
        private OutputClause output;
        private final List<SelectItem> returning = new ArrayList<>();
        private final List<String> optimizerHints = new ArrayList<>();

        /**
         * Creates a builder initialized with a target table.
         *
         * @param table target table
         */
        private Builder(Table table) {
            this.table = Objects.requireNonNull(table, "table");
        }

        /**
         * Creates a builder initialized from an existing update statement snapshot.
         *
         * @param statement source update statement
         */
        private Builder(UpdateStatement statement) {
            this(Objects.requireNonNull(statement, "statement").table());
            this.assignments.addAll(statement.assignments());
            this.joins.addAll(statement.joins());
            this.from.addAll(statement.from());
            this.where = statement.where();
            this.output = statement.output();
            this.returning.addAll(statement.returning());
            this.optimizerHints.addAll(statement.optimizerHints());
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
         * @param column target column qualified name
         * @param value assigned expression
         * @return this builder
         */
        public Builder set(QualifiedName column, Expression value) {
            return set(Assignment.of(column, value));
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
         * Sets the optional SQL Server {@code OUTPUT} clause.
         *
         * @param output output clause or {@code null}
         * @return this builder
         */
        public Builder output(OutputClause output) {
            this.output = output;
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
         * Replaces optimizer hints attached to this statement.
         *
         * @param optimizerHints optimizer hints (without comment delimiters)
         * @return this builder
         */
        public Builder optimizerHints(List<String> optimizerHints) {
            Objects.requireNonNull(optimizerHints, "optimizerHints");
            this.optimizerHints.clear();
            this.optimizerHints.addAll(optimizerHints);
            return this;
        }

        /**
         * Appends one optimizer hint.
         *
         * @param optimizerHint optimizer hint (without comment delimiters)
         * @return this builder
         */
        public Builder optimizerHint(String optimizerHint) {
            this.optimizerHints.add(Objects.requireNonNull(optimizerHint, "optimizerHint"));
            return this;
        }

        /**
         * Clears optimizer hints attached to this statement.
         *
         * @return this builder
         */
        public Builder clearOptimizerHints() {
            this.optimizerHints.clear();
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
            return UpdateStatement.of(table, assignments, joins, from, where, output, returning, optimizerHints);
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
     * @param output optional SQL Server output clause
     * @param returning optional returning projection items
     * @param optimizerHints optimizer hints (without comment delimiters)
     */
    record Impl(Table table,
                List<Assignment> assignments,
                List<Join> joins,
                List<TableRef> from,
                Predicate where,
                OutputClause output,
                List<SelectItem> returning,
                List<String> optimizerHints) implements UpdateStatement {
        /**
         * Creates an immutable update statement implementation.
         */
        public Impl {
            Objects.requireNonNull(table, "table");
            assignments = List.copyOf(Objects.requireNonNull(assignments, "assignments"));
            joins = joins == null ? List.of() : List.copyOf(joins);
            from = from == null ? List.of() : List.copyOf(from);
            returning = returning == null ? List.of() : List.copyOf(returning);
            optimizerHints = optimizerHints == null ? List.of() : List.copyOf(optimizerHints);
            if (assignments.isEmpty()) {
                throw new IllegalArgumentException("assignments must not be empty");
            }
        }
    }
}
