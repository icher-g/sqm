package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models a dialect-neutral {@code DELETE} statement.
 */
public non-sealed interface DeleteStatement extends Statement {

    /**
     * Creates an immutable delete statement.
     *
     * @param table target table
     * @param using optional USING sources
     * @param joins optional joined sources attached to the USING clause
     * @param where optional predicate
     * @param output optional SQL Server output clause
     * @param returning optional returning projection items
     * @param optimizerHints optimizer hints (without comment delimiters)
     * @return immutable delete statement
     */
    static DeleteStatement of(Table table,
                              List<TableRef> using,
                              List<Join> joins,
                              Predicate where,
                              OutputClause output,
                              List<SelectItem> returning,
                              List<String> optimizerHints) {
        return new Impl(table, using, joins, where, output, returning, optimizerHints);
    }

    /**
     * Creates a mutable builder for constructing immutable delete statements.
     *
     * @param table target table
     * @return builder initialized with the target table
     */
    static Builder builder(Table table) {
        return new Builder(table);
    }

    /**
     * Creates a mutable builder initialized from an existing delete statement snapshot.
     *
     * @param statement source delete statement
     * @return builder initialized with the statement state
     */
    static Builder builder(DeleteStatement statement) {
        return new Builder(statement);
    }

    /**
     * Returns the delete target table.
     *
     * @return target table
     */
    Table table();

    /**
     * Returns optional {@code USING} sources.
     *
     * @return immutable using source list
     */
    List<TableRef> using();

    /**
     * Returns optional joined sources attached to the USING clause.
     *
     * @return immutable join list
     */
    List<Join> joins();

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
        return visitor.visitDeleteStatement(this);
    }

    /**
     * Mutable builder for constructing immutable {@link DeleteStatement} instances.
     */
    final class Builder {
        private Table table;
        private final List<TableRef> using = new ArrayList<>();
        private final List<Join> joins = new ArrayList<>();
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
         * Creates a builder initialized from an existing delete statement snapshot.
         *
         * @param statement source delete statement
         */
        private Builder(DeleteStatement statement) {
            this(Objects.requireNonNull(statement, "statement").table());
            this.using.addAll(statement.using());
            this.joins.addAll(statement.joins());
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
         * Replaces optional {@code USING} sources.
         *
         * @param using using sources
         * @return this builder
         */
        public Builder using(List<TableRef> using) {
            Objects.requireNonNull(using, "using");
            this.using.clear();
            this.using.addAll(using);
            return this;
        }

        /**
         * Replaces optional {@code USING} sources.
         *
         * @param using using sources
         * @return this builder
         */
        public Builder using(TableRef... using) {
            Objects.requireNonNull(using, "using");
            return using(List.of(using));
        }

        /**
         * Replaces joined sources attached to the USING clause.
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
         * Replaces joined sources attached to the USING clause.
         *
         * @param joins joins to use
         * @return this builder
         */
        public Builder joins(Join... joins) {
            Objects.requireNonNull(joins, "joins");
            return joins(List.of(joins));
        }

        /**
         * Appends one join attached to the USING clause.
         *
         * @param join join to append
         * @return this builder
         */
        public Builder join(Join join) {
            this.joins.add(Objects.requireNonNull(join, "join"));
            return this;
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
         * Builds an immutable delete statement.
         *
         * @return immutable delete statement
         */
        public DeleteStatement build() {
            if (table == null) {
                throw new IllegalStateException("table must be set");
            }
            return DeleteStatement.of(table, using, joins, where, output, returning, optimizerHints);
        }
    }

    /**
     * Default immutable delete statement implementation.
     *
     * @param table target table
     * @param using optional using sources
     * @param joins optional joined sources attached to the USING clause
     * @param where optional predicate
     * @param output optional SQL Server output clause
     * @param returning optional returning projection items
     * @param optimizerHints optimizer hints (without comment delimiters)
     */
    record Impl(Table table,
                List<TableRef> using,
                List<Join> joins,
                Predicate where,
                OutputClause output,
                List<SelectItem> returning,
                List<String> optimizerHints) implements DeleteStatement {
        /**
         * Creates an immutable delete statement implementation.
         */
        public Impl {
            Objects.requireNonNull(table, "table");
            using = using == null ? List.of() : List.copyOf(using);
            joins = joins == null ? List.of() : List.copyOf(joins);
            returning = returning == null ? List.of() : List.copyOf(returning);
            optimizerHints = optimizerHints == null ? List.of() : List.copyOf(optimizerHints);
        }
    }
}

