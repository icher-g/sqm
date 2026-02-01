package io.sqm.core.dialect;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Dialect capabilities implementation with minimum-version support per feature.
 * <p>
 * Each feature is mapped to the minimum version of the dialect that supports it.
 */
public final class VersionedDialectCapabilities implements DialectCapabilities {
    private final SqlDialectVersion version;
    private final EnumMap<SqlFeature, SqlDialectVersion> minVersions;

    private VersionedDialectCapabilities(SqlDialectVersion version, EnumMap<SqlFeature, SqlDialectVersion> minVersions) {
        this.version = Objects.requireNonNull(version, "version");
        this.minVersions = new EnumMap<>(minVersions);
    }

    /**
     * Creates a builder for the given dialect version.
     *
     * @param version dialect version to evaluate feature availability
     * @return a builder instance
     */
    public static Builder builder(SqlDialectVersion version) {
        return new Builder(version);
    }

    /**
     * Returns the dialect version associated with this capability set.
     *
     * @return dialect version
     */
    public SqlDialectVersion version() {
        return version;
    }

    /**
     * Returns {@code true} if the current version supports the given feature.
     *
     * @param feature feature to check
     * @return {@code true} if supported, {@code false} otherwise
     */
    @Override
    public boolean supports(SqlFeature feature) {
        SqlDialectVersion minVersion = minVersions.get(feature);
        return minVersion != null && version.isAtLeast(minVersion);
    }

    /**
     * Builder for {@link VersionedDialectCapabilities}.
     */
    public static final class Builder {
        private final SqlDialectVersion version;
        private final EnumMap<SqlFeature, SqlDialectVersion> minVersions = new EnumMap<>(SqlFeature.class);

        private Builder(SqlDialectVersion version) {
            this.version = Objects.requireNonNull(version, "version");
        }

        /**
         * Registers a feature as supported since the minimal dialect version.
         *
         * @param feature feature to register
         * @return this builder instance
         */
        public Builder supports(SqlFeature feature) {
            return supports(feature, SqlDialectVersion.minimum());
        }

        /**
         * Registers a feature as supported since the provided minimum version.
         *
         * @param feature    feature to register
         * @param minVersion minimum version that supports the feature
         * @return this builder instance
         */
        public Builder supports(SqlFeature feature, SqlDialectVersion minVersion) {
            minVersions.put(Objects.requireNonNull(feature, "feature"), Objects.requireNonNull(minVersion, "minVersion"));
            return this;
        }

        /**
         * Registers multiple features with the same minimum version.
         *
         * @param minVersion minimum version that supports the features
         * @param features   features to register
         * @return this builder instance
         */
        public Builder supports(SqlDialectVersion minVersion, SqlFeature... features) {
            Objects.requireNonNull(features, "features");
            for (SqlFeature feature : features) {
                supports(feature, minVersion);
            }
            return this;
        }

        /**
         * Builds an immutable {@link VersionedDialectCapabilities} instance.
         *
         * @return a new capabilities instance
         */
        public VersionedDialectCapabilities build() {
            return new VersionedDialectCapabilities(version, minVersions);
        }
    }

    /**
     * Returns a snapshot of the configured feature-to-version mapping.
     *
     * @return immutable view of the feature minimum versions
     */
    public Map<SqlFeature, SqlDialectVersion> minVersions() {
        return Map.copyOf(minVersions);
    }
}
