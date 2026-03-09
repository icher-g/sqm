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
        return new Impl(table, columns, source, List.of());
    }

    /**
     * Creates an immutable insert statement with a {@code RETURNING} projection list.
     *
     * @param table target table
     * @param columns target columns, or empty when omitted
     * @param source insert source
     * @param returning returning projection list, or empty when omitted
     * @return immutable insert statement
     */
    static InsertStatement of(Table table, List<Identifier> columns, InsertSource source, List<SelectItem> returning) {
        return new Impl(table, columns, source, returning);
    }

    /**
     * Creates an immutable insert statement without an explicit column list.
     *
     * @param table target table
     * @param source insert source
     * @return immutable insert statement
     */
    static InsertStatement of(Table table, InsertSource source) {
        return new Impl(table, List.of(), source, List.of());
    }

    /**
     * Creates an immutable insert statement without an explicit column list and with
     * optional {@code RETURNING} items.
     *
     * @param table target table
     * @param source insert source
     * @param returning returning projection list, or empty when omitted
     * @return immutable insert statement
     */
    static InsertStatement of(Table table, InsertSource source, List<SelectItem> returning) {
        return new Impl(table, List.of(), source, returning);
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
     * Returns optional {@code RETURNING} projection items.
     *
     * @return immutable returning item list
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
        return visitor.visitInsertStatement(this);
    }

    /**
     * Mutable builder for constructing immutable {@link InsertStatement} instances.
     */
    final class Builder {
        private Table table;
        private final List<Identifier> columns = new ArrayList<>();
        private InsertSource source;
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
         * Replaces the {@code RETURNING} projection list.
         *
         * @param returning returning projection list
         * @return this builder
         */
        public Builder returning(List<SelectItem> returning) {
            Objects.requireNonNull(returning, "returning");
            this.returning.clear();
            this.returning.addAll(returning);
            return this;
        }

        /**
         * Replaces the {@code RETURNING} projection list.
         *
         * @param returning returning projection items
         * @return this builder
         */
        public Builder returning(SelectItem... returning) {
            Objects.requireNonNull(returning, "returning");
            return returning(List.of(returning));
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
            return InsertStatement.of(table, columns, source, returning);
        }
    }

    /**
     * Default immutable implementation of {@link InsertStatement}.
     *
     * @param table target table
     * @param columns target columns
     * @param source insert source
     * @param returning returning projection list
     */
    record Impl(Table table, List<Identifier> columns, InsertSource source, List<SelectItem> returning) implements InsertStatement {
        /**
         * Creates an immutable insert statement implementation.
         */
        public Impl {
            Objects.requireNonNull(table, "table");
            Objects.requireNonNull(source, "source");
            columns = columns == null ? List.of() : List.copyOf(columns);
            returning = returning == null ? List.of() : List.copyOf(returning);
        }
    }
}
