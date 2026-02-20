package io.sqm.core.control;

import java.io.Serializable;
import java.util.Objects;

/**
 * Pair of decision result and explanation text.
 *
 * @param decision    decision result
 * @param explanation explanation text
 */
public record DecisionExplanation(
    DecisionResult decision,
    String explanation
) implements Serializable {

    /**
     * Validates fields.
     *
     * @param decision    decision result
     * @param explanation explanation text
     */
    public DecisionExplanation {
        Objects.requireNonNull(decision, "decision must not be null");
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("explanation must not be blank");
        }
    }
}
