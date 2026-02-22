package io.sqm.control;

import java.util.Objects;

/**
 * Exception used by rewrite rules to request a deterministic deny decision from the decision engine.
 */
public final class RewriteDenyException extends RuntimeException {
    private final ReasonCode reasonCode;

    /**
     * Creates a rewrite deny exception.
     *
     * @param reasonCode stable reason code for the deny decision
     * @param message deny explanation message
     */
    public RewriteDenyException(ReasonCode reasonCode, String message) {
        super(message);
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null");
    }

    /**
     * Returns the deny reason code requested by the rewrite rule.
     *
     * @return deny reason code
     */
    public ReasonCode reasonCode() {
        return reasonCode;
    }
}
