package io.sqm.validate.api;

import java.util.List;
import java.util.Objects;

/**
 * Validation result with all collected semantic problems.
 *
 * @param problems immutable list of discovered problems.
 */
public record ValidationResult(List<ValidationProblem> problems) {
    /**
     * Creates a validation result.
     *
     * @param problems problems list.
     */
    public ValidationResult {
        Objects.requireNonNull(problems, "problems");
        problems = List.copyOf(problems);
    }

    /**
     * Returns whether validation succeeded without problems.
     *
     * @return {@code true} when no problems were found.
     */
    public boolean ok() {
        return problems.isEmpty();
    }
}
