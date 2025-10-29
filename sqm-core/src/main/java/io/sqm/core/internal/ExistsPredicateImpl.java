package io.sqm.core.internal;

import io.sqm.core.ExistsPredicate;
import io.sqm.core.Query;

public record ExistsPredicateImpl(Query subquery, boolean negated) implements ExistsPredicate {
}
