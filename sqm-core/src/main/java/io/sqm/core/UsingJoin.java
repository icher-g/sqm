package io.sqm.core;

import java.util.List;

/**
 * Represents a USING JOIN statement.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM orders
 *     JOIN customers USING (customer_id, region_id);
 *     }
 * </pre>
 */
public non-sealed interface UsingJoin extends Join {
    /**
     * USING (col1, col2, ...);
     */
    List<String> usingColumns();
}
