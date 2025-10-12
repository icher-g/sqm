package io.cherlabs.sqm.core;

import java.util.List;
import java.util.Objects;

/**
 * Sum type for filter RHS values (for IN, BETWEEN, subqueries, etc.).
 */
public sealed interface Values extends Entity permits Values.ListValues, Values.Range, Values.Single, Values.Column, Values.Subquery, Values.Tuples {

    /**
     * Creates a single value.
     *
     * @param value a value.
     * @return A new instance of Values.
     */
    static Single single(Object value) {
        return new Single(value);
    }

    /**
     * Creates a column value.
     *
     * @param column a column.
     * @return A new instance of Values.
     */
    static Column column(io.cherlabs.sqm.core.Column column) {
        return new Column(column);
    }

    /**
     * Creates a list of values.
     *
     * @param items a list of values.
     * @return A new instance of Values.
     */
    static ListValues list(List<?> items) {
        return new ListValues(items);
    }

    /**
     * Creates a list of tuples.
     *
     * @param rows a list of tuples.
     * @return A new instance of Values.
     */
    static Tuples tuples(List<? extends List<?>> rows) {
        return new Tuples(rows);
    }

    /**
     * Creates a range value.
     *
     * @param min a minimum value.
     * @param max a maximum value.
     * @return A new instance of Values.
     */
    static Range range(Object min, Object max) {
        return new Range(min, max);
    }

    /**
     * Creates a query value.
     *
     * @param q a query.
     * @return A new instance of Values.
     */
    static Subquery subquery(Query q) {
        return new Subquery(q);
    }

    /**
     * Single‑column =: {@code col = 1 }.
     */
    record Single(Object value) implements Values {
    }

    /**
     * Single‑column IN: {@code col IN (1,2,3)}.
     */
    record ListValues(List<?> items) implements Values {
        public ListValues {
            items = List.copyOf(Objects.requireNonNull(items));
        }
    }

    /**
     * Multi‑column tuple IN: {@code (a,b) IN ((1,2),(3,4))}.
     */
    record Tuples(List<? extends List<?>> rows) implements Values {
        public Tuples {
            Objects.requireNonNull(rows);
            rows = List.copyOf(rows.stream().map(List::copyOf).toList());
        }
    }

    /**
     * Range (usually for BETWEEN): {@code col BETWEEN min AND max}.
     */
    record Range(Object min, Object max) implements Values {
        public Range {
            Objects.requireNonNull(min);
            Objects.requireNonNull(max);
        }
    }

    /**
     * Column: {@code col = col}.
     */
    record Column(io.cherlabs.sqm.core.Column column) implements Values {
        public Column {
            Objects.requireNonNull(column);
        }
    }

    /**
     * Subquery: {@code col IN (SELECT ...)}.
     */
    record Subquery(Query query) implements Values {
        public Subquery {
            Objects.requireNonNull(query);
        }
    }
}
