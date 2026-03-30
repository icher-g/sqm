package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Represents a target that receives rows produced by a {@link ResultClause}.
 * <p>
 * This is primarily intended for dialects such as T-SQL that support redirecting mutation results
 * into another table or table variable, for example:
 *
 * <pre>{@code
 * OUTPUT INSERTED.id, INSERTED.name INTO audit_table(id, name)
 * }</pre>
 */
public non-sealed interface ResultInto extends Node {

    /**
     * Creates a result-into target without an explicit target column list.
     *
     * @param target target relation
     * @return result-into specification
     */
    static ResultInto of(TableRef target) {
        return of(target, List.of());
    }

    /**
     * Creates a result-into target specification.
     *
     * @param target  target relation
     * @param columns optional target columns
     * @return result-into specification
     */
    static ResultInto of(TableRef target, List<Identifier> columns) {
        return new Impl(target, columns);
    }

    /**
     * Returns the target relation that receives the emitted rows.
     *
     * @return result target table
     */
    TableRef target();

    /**
     * Returns the optional target column list.
     *
     * @return immutable target column list
     */
    List<Identifier> columns();

    /**
     * Returns whether this target writes into a catalog-backed base table.
     *
     * @return {@code true} when {@link #target()} is a {@link Table}
     */
    default boolean isBaseTableTarget() {
        return target().<Boolean>matchTableRef().table(table -> true).orElse(false);
    }

    /**
     * Returns whether this target writes into a table variable.
     *
     * @return {@code true} when {@link #target()} is a {@link VariableTableRef}
     */
    default boolean isVariableTarget() {
        return target().<Boolean>matchTableRef().variableTable(table -> true).orElse(false);
    }

    /**
     * Returns whether this target writes into a derived relation such as a subquery or function table.
     *
     * @return {@code true} when {@link #target()} is not a base table and not a table variable
     */
    default boolean isDerivedTarget() {
        return !isBaseTableTarget() && !isVariableTarget();
    }

    /**
     * Accepts a visitor.
     *
     * @param v   visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitResultInto(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param target  target relation
     * @param columns optional target columns
     */
    record Impl(TableRef target, List<Identifier> columns) implements ResultInto {

        /**
         * Creates a result-into implementation.
         *
         * @param target  target relation
         * @param columns optional target columns
         */
        public Impl {
            Objects.requireNonNull(target, "target");
            columns = columns == null ? List.of() : List.copyOf(columns);
        }
    }
}
