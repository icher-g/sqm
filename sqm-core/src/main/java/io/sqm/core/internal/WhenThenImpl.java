package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.Predicate;
import io.sqm.core.WhenThen;

/**
 * Represents WHEN...THEN statement used in CASE expression.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END AS result
 *     }
 * </pre>
 *
 * @param when a WHEN predicate.
 * @param then a THEN expression.
 */
public record WhenThenImpl(Predicate when, Expression then) implements WhenThen {
}
