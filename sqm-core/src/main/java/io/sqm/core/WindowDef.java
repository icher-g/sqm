package io.sqm.core;

import io.sqm.core.internal.WindowDefImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a WINDOW specification used in OVER clause.
 * <p>Example of WINDOW specification:</p>
 * <pre>
 *     {@code
 *      SELECT
 *          dept,
 *          emp_name,
 *          salary,
 *          RANK() OVER w1        AS dept_rank,
 *          AVG(salary) OVER w2   AS overall_avg
 *      FROM employees
 *      WINDOW
 *          w1 AS (PARTITION BY dept ORDER BY salary DESC),
 *          w2 AS (ORDER BY salary DESC);
 *     }
 * </pre>
 */
public non-sealed interface WindowDef extends Node {

    /**
     * Creates an instance of the WINDOW definition with the provided name and OVER specification.
     *
     * @param name a window name.
     * @param spec an OVER specification.
     * @return new instance of the WINDOW definition.
     */
    static WindowDef of(String name, OverSpec.Def spec) {
        return new WindowDefImpl(name, spec);
    }

    /**
     * Gets the name of the WINDOW.
     *
     * @return the name of the WINDOW.
     */
    String name();

    /**
     * Gets the OVER specification.
     *
     * @return OVER specification.
     */
    OverSpec.Def spec();

    /**
     * Accepts a {@link NodeVisitor} that performs an operation on this node.
     * <p>
     * Each concrete node class calls back into the visitor with a type-specific
     * {@code visitXxx(...)} method, allowing the visitor to handle each node
     * type appropriately.
     * </p>
     *
     * @param v the visitor instance to accept (must not be {@code null})
     * @return the result of the visitor’s operation on this node,
     * or {@code null} if the visitor’s return type is {@link Void}
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitWindowDef(this);
    }
}
