package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.GroupItem;

/**
 * Either expression-based grouping or ordinal reference. Exactly one is non-null.
 *
 * @param expr    an expression.
 * @param ordinal an ordinal.
 */
public record GroupItemImpl(Expression expr, Integer ordinal) implements GroupItem {
}
