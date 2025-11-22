package io.sqm.core.internal;

import io.sqm.core.NamedParamExpr;

/**
 * A parameter identified by a symbolic name rather than a numeric index.
 * <p>
 * Named parameters may appear in different syntactic forms depending on the
 * source SQL dialect (e.g., {@code :id}, {@code @id}, {@code #{id}}).
 * SQM stores only the canonical name without any prefix.
 *
 * @param name the canonical name of the parameter (e.g. {@code "id"}).
 */
public record NamedParamExprImpl(String name) implements NamedParamExpr {
}
