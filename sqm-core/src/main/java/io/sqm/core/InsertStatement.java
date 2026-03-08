package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models a dialect-neutral {@code INSERT} statement.
 */
public non-sealed interface InsertStatement extends Statement {

    /**
     * Creates an immutable insert statement.
     *
     * @param table target table
     * @param columns target columns, or empty when omitted
     * @param source insert source
     * @return immutable insert statement
     */
    static InsertStatement of(Table table, List<Identifier> columns, InsertSource source) {
        return new Impl(table, columns, source);
    }

    /**
     * Creates an immutable insert statement without an explicit column list.
     *
     * @param table target table
     * @param source insert source
     * @return immutable insert statement
     */
    static InsertStatement of(Table table, InsertSource source) {
        return new Impl(table, List.of(), source);
    }

    /**
     * Creates a mutable builder for constructing immutable insert statements.
     *
     * @param table target table
     * @return builder initialized with the target table
     */
    static Builder builder(Table table) {
        return new Builder(table);
    }

    /**
     * Returns the insert target table.
     *
     * @return target table
     */
    Table table();

    /**
     * Returns explicitly targeted columns.
     *
     * @return immutable target column list
     */
    List<Identifier> columns();

    /**
     * Returns the insert source.
     *
     * @return insert source
     */
    InsertSource source();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitInsertStatement(this);
    }

    /**
     * Mutable builder for constructing immutable {@link InsertStatement} instances.
     */
    final class Builder {
        private Table table;
        private final List<Identifier> columns = new ArrayList<>();
        private InsertSource source;

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
         * Replaces the target column list.
         *
         * @param columns target columns
         * @return this builder
         */
        public Builder columns(List<Identifier> columns) {
            Objects.requireNonNull(columns, "columns");
            this.columns.clear();
            this.columns.addAll(columns);
            return this;
        }

        /**
         * Replaces the target column list.
         *
         * @param columns target columns
         * @return this builder
         */
        public Builder columns(Identifier... columns) {
            Objects.requireNonNull(columns, "columns");
            return columns(List.of(columns));
        }

        /**
         * Sets the insert source.
         *
         * @param source insert source
         * @return this builder
         */
        public Builder source(InsertSource source) {
            this.source = Objects.requireNonNull(source, "source");
            return this;
        }

        /**
         * Sets a row-values insert source.
         *
         * @param values row values source
         * @return this builder
         */
        public Builder values(RowValues values) {
            return source(values);
        }

        /**
         * Sets a query insert source.
         *
         * @param query query source
         * @return this builder
         */
        public Builder query(Query query) {
            return source(query);
        }

        /**
         * Builds an immutable insert statement.
         *
         * @return immutable insert statement
         */
        public InsertStatement build() {
            if (table == null) {
                throw new IllegalStateException("table must be set");
            }
            if (source == null) {
                throw new IllegalStateException("source must be set");
            }
            return InsertStatement.of(table, columns, source);
        }
    }

    /**
     * Default immutable implementation of {@link InsertStatement}.
     *
     * @param table target table
     * @param columns target columns
     * @param source insert source
     */
    record Impl(Table table, List<Identifier> columns, InsertSource source) implements InsertStatement {
        /**
         * Creates an immutable insert statement implementation.
         */
        public Impl {
            Objects.requireNonNull(table, "table");
            Objects.requireNonNull(source, "source");
            columns = columns == null ? List.of() : List.copyOf(columns);
        }
    }
}
