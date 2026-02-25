package io.sqm.control;

/**
 * Result of query validation in middleware decision flow.
 *
 * @param code    validation outcome reason code
 * @param message validation message for failed outcomes
 */
public record QueryValidateResult(ReasonCode code, String message) {

    /**
     * Creates a successful validation result.
     *
     * @return successful validation result
     */
    public static QueryValidateResult ok() {
        return new QueryValidateResult(ReasonCode.NONE, null);
    }

    /**
     * Creates a failed validation result.
     *
     * @param code    failure reason code
     * @param message failure message
     * @return failed validation result
     */
    public static QueryValidateResult failure(ReasonCode code, String message) {
        return new QueryValidateResult(code, message);
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
