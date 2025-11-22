package io.sqm.core.internal;

import io.sqm.core.OrdinalParamExpr;

/**
 * A parameter that carries an explicit ordinal index.
 * <p>
 * Examples from different SQL environments include:
 * <ul>
 *   <li>{@code $1}, {@code $2}, ...}</li>
 *   <li>{@code ?1}, {@code ?2}, ...}</li>
 * </ul>
 * Regardless of how the parameter appeared in the original SQL text,
 * SQM only preserves the semantic information – the index itself.
 * The renderer for each dialect determines how the index is printed.
 *
 * @param index the explicit 1-based index of this parameter.
 */
public record OrdinalParamExprImpl(int index) implements OrdinalParamExpr {
}
