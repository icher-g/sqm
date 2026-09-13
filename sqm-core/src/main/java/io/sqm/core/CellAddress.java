package io.sqm.core;

import io.sqm.core.match.CellAddressMatch;

/**
 * Address of one or more cells in a MODEL rule target.
 *
 * <p>Each bracket entry is an address. Here {@code 'Bike'} is a {@link CellSelector.Value}, and {@code FOR
 * sales_year IN (2026, 2027)} is a {@link CellFor.Values}.</p>
 * <pre>{@code
 * SELECT product, sales_year, sales
 * FROM sales_data
 * MODEL
 *   DIMENSION BY (product, sales_year)
 *   MEASURES (amount AS sales)
 *   RULES (
 *     sales['Bike', FOR sales_year IN (2026, 2027)] = 100
 *   )
 * }</pre>
 */
public sealed interface CellAddress extends Node permits CellSelector, CellFor {
    /**
     * Creates a matcher for selector or generator addresses.
     *
     * @param <R> result type
     * @return address matcher
     */
    default <R> CellAddressMatch<R> matchCellAddress() {
        return CellAddressMatch.match(this);
    }
}
