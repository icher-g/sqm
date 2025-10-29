package io.sqm.core;

/**
 * This is a marker interface for composite predicates such as 'AND' and 'OR'.
 */
public sealed interface CompositePredicate extends Predicate permits AndPredicate, OrPredicate {
}
