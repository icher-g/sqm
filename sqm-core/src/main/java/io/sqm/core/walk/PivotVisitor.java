package io.sqm.core.walk;

import io.sqm.core.PivotMeasure;
import io.sqm.core.PivotValue;
import io.sqm.core.UnpivotInput;

/**
 * Visitor for pivot and unpivot helper nodes.
 *
 * @param <R> visitor result type
 */
public interface PivotVisitor<R> {
    /**
     * Visits a pivot measure.
     *
     * @param measure pivot measure
     * @return visitor result
     */
    R visitPivotMeasure(PivotMeasure measure);

    /**
     * Visits a pivot value.
     *
     * @param value pivot value
     * @return visitor result
     */
    R visitPivotValue(PivotValue value);

    /**
     * Visits an unpivot input.
     *
     * @param input unpivot input
     * @return visitor result
     */
    R visitUnpivotInput(UnpivotInput input);
}
