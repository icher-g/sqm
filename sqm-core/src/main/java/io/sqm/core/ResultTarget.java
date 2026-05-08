package io.sqm.core;

/**
 * Marker interface for a target that receives or exposes rows produced by a {@link ResultClause}.
 * <p>
 * Direct caller-visible result rows are represented by a {@code null} target on the result clause.
 */
public sealed interface ResultTarget extends Node permits RelationResultTarget, VariableResultTarget {
}
