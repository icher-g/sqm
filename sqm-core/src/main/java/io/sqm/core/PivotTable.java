package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Relational table reference that pivots row values into columns.
 */
public non-sealed interface PivotTable extends TableRef {
    /**
     * Creates a pivot table reference.
     *
     * @param source source relation to pivot
     * @param measures aggregate measures to produce
     * @param forExpression expression whose values become output columns
     * @param values explicit pivot values
     * @return pivot table reference
     */
    static PivotTable of(TableRef source, List<PivotMeasure> measures, Expression forExpression, List<PivotValue> values) {
        return of(source, measures, forExpression, values, null);
    }

    /**
     * Creates a pivot table reference.
     *
     * @param source source relation to pivot
     * @param measures aggregate measures to produce
     * @param forExpression expression whose values become output columns
     * @param values explicit pivot values
     * @param alias optional table alias
     * @return pivot table reference
     */
    static PivotTable of(TableRef source, List<PivotMeasure> measures, Expression forExpression, List<PivotValue> values, Identifier alias) {
        return new Impl(source, measures, forExpression, values, alias);
    }

    /**
     * Returns the source relation to pivot.
     *
     * @return source relation
     */
    TableRef source();

    /**
     * Returns aggregate measures to produce.
     *
     * @return immutable measure list
     */
    List<PivotMeasure> measures();

    /**
     * Returns the expression whose values become output columns.
     *
     * @return pivot-for expression
     */
    Expression forExpression();

    /**
     * Returns explicit pivot values.
     *
     * @return immutable pivot value list
     */
    List<PivotValue> values();

    /**
     * Returns the optional table alias.
     *
     * @return alias or {@code null}
     */
    Identifier alias();

    /**
     * Creates a copy with the provided table alias.
     *
     * @param alias table alias
     * @return pivot table reference with alias
     */
    default PivotTable as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Creates a copy with the provided table alias.
     *
     * @param alias table alias
     * @return pivot table reference with alias
     */
    default PivotTable as(Identifier alias) {
        return of(source(), measures(), forExpression(), values(), alias);
    }

    /**
     * Accepts a node visitor.
     *
     * @param v visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitPivotTable(this);
    }

    /**
     * Immutable pivot table implementation.
     *
     * @param source source relation to pivot
     * @param measures aggregate measures to produce
     * @param forExpression expression whose values become output columns
     * @param values explicit pivot values
     * @param alias optional table alias
     */
    record Impl(TableRef source,
                List<PivotMeasure> measures,
                Expression forExpression,
                List<PivotValue> values,
                Identifier alias) implements PivotTable {
        /**
         * Creates an immutable pivot table reference.
         */
        public Impl {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(measures, "measures");
            Objects.requireNonNull(forExpression, "forExpression");
            Objects.requireNonNull(values, "values");
            if (measures.isEmpty()) {
                throw new IllegalArgumentException("Pivot requires at least one measure");
            }
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Pivot requires at least one value");
            }
            measures = List.copyOf(measures);
            values = List.copyOf(values);
        }
    }
}
