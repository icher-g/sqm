package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Relational table reference that turns columns into rows.
 */
public non-sealed interface UnpivotTable extends TableRef {
    /**
     * Creates an unpivot table reference.
     *
     * @param source source relation to unpivot
     * @param valueColumns output value columns
     * @param nameColumn output name column
     * @param inputs input column groups and labels
     * @param nullTreatment null-row treatment
     * @return unpivot table reference
     */
    static UnpivotTable of(TableRef source,
        List<Identifier> valueColumns,
        Identifier nameColumn,
        List<UnpivotInput> inputs,
        NullTreatment nullTreatment) {
        return of(source, valueColumns, nameColumn, inputs, nullTreatment, null);
    }

    /**
     * Creates an unpivot table reference.
     *
     * @param source source relation to unpivot
     * @param valueColumns output value columns
     * @param nameColumn output name column
     * @param inputs input column groups and labels
     * @param nullTreatment null-row treatment
     * @param alias optional table alias
     * @return unpivot table reference
     */
    static UnpivotTable of(TableRef source,
        List<Identifier> valueColumns,
        Identifier nameColumn,
        List<UnpivotInput> inputs,
        NullTreatment nullTreatment,
        Identifier alias) {
        return new Impl(source, valueColumns, nameColumn, inputs, nullTreatment, alias);
    }

    /**
     * Returns the source relation to unpivot.
     *
     * @return source relation
     */
    TableRef source();

    /**
     * Returns output value columns.
     *
     * @return immutable output value column list
     */
    List<Identifier> valueColumns();

    /**
     * Returns the output name column.
     *
     * @return output name column
     */
    Identifier nameColumn();

    /**
     * Returns input column groups and labels.
     *
     * @return immutable input list
     */
    List<UnpivotInput> inputs();

    /**
     * Returns the null-row treatment.
     *
     * @return null treatment
     */
    NullTreatment nullTreatment();

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
     * @return unpivot table reference with alias
     */
    default UnpivotTable as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Creates a copy with the provided table alias.
     *
     * @param alias table alias
     * @return unpivot table reference with alias
     */
    default UnpivotTable as(Identifier alias) {
        return of(source(), valueColumns(), nameColumn(), inputs(), nullTreatment(), alias);
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
        return v.visitUnpivotTable(this);
    }

    /**
     * Null-row behavior for unpivot output.
     */
    enum NullTreatment {
        /**
         * Use dialect default null handling.
         */
        DIALECT_DEFAULT,
        /**
         * Include rows whose unpivoted value columns are null.
         */
        INCLUDE_NULLS,
        /**
         * Exclude rows whose unpivoted value columns are null.
         */
        EXCLUDE_NULLS
    }

    /**
     * Immutable unpivot table implementation.
     *
     * @param source source relation to unpivot
     * @param valueColumns output value columns
     * @param nameColumn output name column
     * @param inputs input column groups and labels
     * @param nullTreatment null-row treatment
     * @param alias optional table alias
     */
    record Impl(TableRef source,
                List<Identifier> valueColumns,
                Identifier nameColumn,
                List<UnpivotInput> inputs,
                NullTreatment nullTreatment,
                Identifier alias) implements UnpivotTable {
        /**
         * Creates an immutable unpivot table reference.
         */
        public Impl {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(valueColumns, "valueColumns");
            Objects.requireNonNull(nameColumn, "nameColumn");
            Objects.requireNonNull(inputs, "inputs");
            if (valueColumns.isEmpty()) {
                throw new IllegalArgumentException("Unpivot requires at least one value column");
            }
            if (inputs.isEmpty()) {
                throw new IllegalArgumentException("Unpivot requires at least one input");
            }
            valueColumns = List.copyOf(valueColumns);
            inputs = List.copyOf(inputs);
            nullTreatment = nullTreatment == null ? NullTreatment.DIALECT_DEFAULT : nullTreatment;
        }
    }
}
