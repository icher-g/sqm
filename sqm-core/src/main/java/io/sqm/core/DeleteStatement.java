package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Models a dialect-neutral {@code DELETE} statement.
 */
public non-sealed interface DeleteStatement extends Statement {

    /**
     * Creates an immutable delete statement.
     *
     * @param table target table
     * @param where optional predicate
     * @return immutable delete statement
     */
    static DeleteStatement of(Table table, Predicate where) {
        return new Impl(table, where);
    }

    /**
     * Creates an immutable delete statement without a {@code WHERE} clause.
     *
     * @param table target table
     * @return immutable delete statement
     */
    static DeleteStatement of(Table table) {
        return new Impl(table, null);
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
     * Returns the delete target table.
     *
     * @return target table
     */
    Table table();

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
        return visitor.visitDeleteStatement(this);
    }

    /**
     * Mutable builder for constructing immutable {@link DeleteStatement} instances.
     */
    final class Builder {
        private Table table;
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
         * Builds an immutable delete statement.
         *
         * @return immutable delete statement
         */
        public DeleteStatement build() {
            if (table == null) {
                throw new IllegalStateException("table must be set");
            }
            return DeleteStatement.of(table, where);
        }
    }

    /**
     * Default immutable delete statement implementation.
     *
     * @param table target table
     * @param where optional predicate
     */
    record Impl(Table table, Predicate where) implements DeleteStatement {
        /**
         * Creates an immutable delete statement implementation.
         */
        public Impl {
            Objects.requireNonNull(table, "table");
        }
    }
}
