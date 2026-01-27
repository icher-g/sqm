package io.sqm.core;

/**
 * Represents a set of row value constructs.
 * <p>
 * This type is used for SQL constructs that operate specifically on row values,
 * such as {@code VALUES} clauses, where the input must be one or more rows.
 * <p>
 * A {@code RowValues} instance may represent:
 * <ul>
 *   <li>a single row value, e.g. {@code (a, b)} ({@link RowExpr})</li>
 *   <li>multiple row values, e.g. {@code (a, b), (c, d)} ({@link RowListExpr})</li>
 * </ul>
 * <p>
 * Although {@link ValueSet} also permits subquery-based value sets
 * (for example, {@code IN (SELECT ...)}), this type intentionally excludes
 * {@link QueryExpr} to reflect SQL grammar restrictions of constructs such as
 * {@code VALUES}, which may contain only row value expressions.
 */
public sealed interface RowValues extends ValueSet
    permits RowExpr, RowListExpr {
}

