package io.sqm.dbit.support;

import java.util.Objects;
import java.util.Set;

/**
 * Declares one executable live-database case and the dialect features it covers.
 *
 * @param id stable case identifier
 * @param features covered dialect features
 * @param execution case body
 * @param <F> dialect feature enum type
 * @param <H> harness type used by the case
 */
public record DialectExecutionCase<F extends Enum<F>, H>(
    String id,
    Set<F> features,
    DialectExecution<H> execution
) {
    /**
     * Creates a live-database execution case.
     *
     * @param id stable case identifier
     * @param features covered dialect features
     * @param execution case body
     */
    public DialectExecutionCase {
        Objects.requireNonNull(id, "id");
        features = Set.copyOf(Objects.requireNonNull(features, "features"));
        if (features.isEmpty()) {
            throw new IllegalArgumentException("features must not be empty");
        }
        Objects.requireNonNull(execution, "execution");
    }
}
