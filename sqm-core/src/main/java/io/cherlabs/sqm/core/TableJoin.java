package io.cherlabs.sqm.core;

import io.cherlabs.sqm.core.traits.HasJoinOn;
import io.cherlabs.sqm.core.traits.HasJoinType;
import io.cherlabs.sqm.core.traits.HasTable;

/**
 * Represent a SQL join.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     INNER JOIN Table1 T1 ON T1.Col1 = B.Col1 AND T1.Col2 = 1
 *     }
 * </pre>
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     INNER JOIN (SELECT * FROM Table1) T1 ON T1.Col1 = B.Col1 AND T1.Col2 = 1
 *     }
 * </pre>
 *
 * @param joinType The join type. INNER from the example.
 * @param table    The name of the joined table. Table1 from the example.
 * @param on       The ON part. T1 ON T1.Col1 = B.Col1 AND T1.Col2 = 1 -> here will be a CompositeSqlFilter.
 */
public record TableJoin(JoinType joinType, Table table, Filter on) implements Join, HasJoinType, HasTable, HasJoinOn {
    /**
     * Creates a join with the provided filter.
     *
     * @param on a filter to be used in a JOIN ON statement.
     * @return A new instance of the join.
     */
    public TableJoin on(Filter on) {
        return new TableJoin(joinType, table, on);
    }
}
