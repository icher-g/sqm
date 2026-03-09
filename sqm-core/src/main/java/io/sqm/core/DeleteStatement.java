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
     * @param where optional predicate
     * @param returning optional returning projection items
     * @return immutable delete statement
     */
    static DeleteStatement of(Table table,
                              List<TableRef> using,
                              Predicate where,
                              List<SelectItem> returning) {
        return new Impl(table, using, where, returning);
    }

    /**
     * Creates an immutable delete statement.
     *
     * @param table target table
     * @param using optional USING sources
     * @param where optional predicate
     * @return immutable delete statement
     */
    static DeleteStatement of(Table table, List<TableRef> using, Predicate where) {
        return of(table, using, where, List.of());
    }


    /**
     * Creates an immutable delete statement.
     *
     * @param table target table
     * @param where optional predicate
     * @return immutable delete statement
     */
    static DeleteStatement of(Table table, Predicate where) {
        return of(table, List.of(), where, List.of());
    }

    /**
     * Creates an immutable delete statement without a {@code WHERE} clause.
     *
     * @param table target table
     * @return immutable delete statement
     */
    static DeleteStatement of(Table table) {
        return of(table, List.of(), null, List.of());
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
     * Returns optional {@code USING} sources.
     *
     * @return immutable using source list
     */
    List<TableRef> using();

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
        return visitor.visitDeleteStatement(this);
    }

    /**
     * Mutable builder for constructing immutable {@link DeleteStatement} instances.
     */
    final class Builder {
        private Table table;
        private final List<TableRef> using = new ArrayList<>();
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
         * Builds an immutable delete statement.
         *
         * @return immutable delete statement
         */
        public DeleteStatement build() {
            if (table == null) {
                throw new IllegalStateException("table must be set");
            }
            return DeleteStatement.of(table, using, where, returning);
        }
    }

    /**
     * Default immutable delete statement implementation.
     *
     * @param table target table
     * @param using optional using sources
     * @param where optional predicate
     * @param returning optional returning projection items
     */
    record Impl(Table table,
                List<TableRef> using,
                Predicate where,
                List<SelectItem> returning) implements DeleteStatement {
        /**
         * Creates an immutable delete statement implementation.
         */
        public Impl {
            Objects.requireNonNull(table, "table");
            using = using == null ? List.of() : List.copyOf(using);
            returning = returning == null ? List.of() : List.copyOf(returning);
        }
    }
}
