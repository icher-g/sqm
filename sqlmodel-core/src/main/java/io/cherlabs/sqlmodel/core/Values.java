package io.cherlabs.sqlmodel.core;

import java.util.List;
import java.util.Objects;

/**
 * Sum type for filter RHS values (for IN, BETWEEN, subqueries, etc.).
 */
public sealed interface Values extends Entity permits Values.ListValues, Values.Range, Values.Single, Values.Column, Values.Subquery, Values.Tuples {

    /* ---------- Factory helpers for nice DSL ---------- */
    static Single single(Object value) {
        return new Single(value);
    }

    static Column column(io.cherlabs.sqlmodel.core.Column column) {
        return new Column(column);
    }

    static ListValues list(List<?> items) {
        return new ListValues(items);
    }

    static Tuples tuples(List<? extends List<?>> rows) {
        return new Tuples(rows);
    }

    static Range range(Object min, Object max) {
        return new Range(min, max);
    }

    static Subquery subquery(Query q) {
        return new Subquery(q);
    }

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
    record Column(io.cherlabs.sqlmodel.core.Column column) implements Values {
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
