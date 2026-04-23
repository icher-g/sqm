package io.sqm.core;

/**
 * Represents the value source used by quantified {@code ANY} and {@code ALL}
 * predicates.
 * <p>
 * Standard quantified comparisons use a {@link Query} source, while dialects
 * such as PostgreSQL can also use an array-producing {@link Expression}.
 * </p>
 */
public sealed interface QuantifiedSource extends Node permits Expression, Query {
}
