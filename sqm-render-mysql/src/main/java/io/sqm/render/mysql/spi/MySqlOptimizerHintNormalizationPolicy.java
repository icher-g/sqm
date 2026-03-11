package io.sqm.render.mysql.spi;

import java.util.Objects;

/**
 * Normalization policy applied to MySQL optimizer-hint bodies during
 * rendering.
 */
public enum MySqlOptimizerHintNormalizationPolicy {
    /**
     * Preserves the stored hint body exactly as provided.
     */
    PASS_THROUGH,

    /**
     * Trims leading and trailing whitespace from the hint body.
     */
    TRIM_OUTER_WHITESPACE,

    /**
     * Trims leading and trailing whitespace and collapses internal whitespace
     * runs to a single space.
     */
    NORMALIZE_WHITESPACE;

    /**
     * Normalizes one optimizer-hint body according to this policy.
     *
     * @param hintBody hint body without comment delimiters.
     * @return normalized hint body.
     */
    public String normalize(String hintBody) {
        Objects.requireNonNull(hintBody, "hintBody");
        return switch (this) {
            case PASS_THROUGH -> hintBody;
            case TRIM_OUTER_WHITESPACE -> hintBody.trim();
            case NORMALIZE_WHITESPACE -> hintBody.trim().replaceAll("\\s+", " ");
        };
    }
}
