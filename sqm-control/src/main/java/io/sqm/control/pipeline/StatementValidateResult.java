package io.sqm.control.pipeline;

import io.sqm.control.decision.ReasonCode;

/**
 * Result of query validation in middleware decision flow.
 *
 * @param code    validation outcome reason code
 * @param message validation message for failed outcomes
 */
public record StatementValidateResult(ReasonCode code, String message) {

    /**
     * Creates a successful validation result.
     *
     * @return successful validation result
     */
    public static StatementValidateResult ok() {
        return new StatementValidateResult(ReasonCode.NONE, null);
    }

    /**
     * Creates a failed validation result.
     *
     * @param code    failure reason code
     * @param message failure message
     * @return failed validation result
     */
    public static StatementValidateResult failure(ReasonCode code, String message) {
        return new StatementValidateResult(code, message);
    }

    /**
     * Returns whether validation failed with problems.
     *
     * @return {@code true} when code reason is different from {@link ReasonCode#NONE}.
     */
    public boolean isFailed() {
        return code != ReasonCode.NONE;
    }
}



