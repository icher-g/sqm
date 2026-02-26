package io.sqm.validate.schema;

/**
 * Structural limits used by schema-query validation.
 *
 * @param maxJoinCount maximum join count per SELECT query, {@code null} means unlimited.
 * @param maxSelectColumns maximum projected column count per SELECT query, {@code null} means unlimited.
 */
public record SchemaValidationLimits(
    Integer maxJoinCount,
    Integer maxSelectColumns
) {
    /**
     * Creates unlimited limits.
     *
     * @return unlimited limits.
     */
    public static SchemaValidationLimits unlimited() {
        return new SchemaValidationLimits(null, null);
    }

    /**
     * Creates a mutable builder.
     *
     * @return limits builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Validates limits invariants.
     *
     * @param maxJoinCount maximum join count.
     * @param maxSelectColumns maximum projected column count.
     */
    public SchemaValidationLimits {
        if (maxJoinCount != null && maxJoinCount < 0) {
            throw new IllegalArgumentException("maxJoinCount must be >= 0");
        }
        if (maxSelectColumns != null && maxSelectColumns < 1) {
            throw new IllegalArgumentException("maxSelectColumns must be >= 1");
        }
    }

    /**
     * Mutable builder for {@link SchemaValidationLimits}.
     */
    public static final class Builder {
        private Integer maxJoinCount;
        private Integer maxSelectColumns;

        /**
         * Creates an empty limits builder.
         */
        public Builder() {
        }

        /**
         * Sets maximum join count allowed per SELECT query.
         *
         * @param maxJoinCount max join count, must be >= 0.
         * @return this builder.
         */
        public Builder maxJoinCount(int maxJoinCount) {
            if (maxJoinCount < 0) {
                throw new IllegalArgumentException("maxJoinCount must be >= 0");
            }
            this.maxJoinCount = maxJoinCount;
            return this;
        }

        /**
         * Sets maximum projected column count allowed per SELECT query.
         *
         * @param maxSelectColumns max projected columns, must be >= 1.
         * @return this builder.
         */
        public Builder maxSelectColumns(int maxSelectColumns) {
            if (maxSelectColumns < 1) {
                throw new IllegalArgumentException("maxSelectColumns must be >= 1");
            }
            this.maxSelectColumns = maxSelectColumns;
            return this;
        }

        /**
         * Builds immutable limits.
         *
         * @return limits instance.
         */
        public SchemaValidationLimits build() {
            return new SchemaValidationLimits(maxJoinCount, maxSelectColumns);
        }
    }
}
