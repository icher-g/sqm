package io.sqm.codegen.maven;

import java.util.Objects;

/**
 * Metadata persisted alongside schema cache snapshots to decide cache reuse safety.
 *
 * @param dialect codegen dialect used when cache was created.
 * @param databaseProduct DB product name, for example {@code PostgreSQL}.
 * @param databaseMajorVersion DB major version.
 * @param generatedAtEpochMillis cache generation timestamp in epoch millis.
 */
record SchemaCacheMetadata(
    String dialect,
    String databaseProduct,
    Integer databaseMajorVersion,
    long generatedAtEpochMillis
) {
    /**
     * Checks whether current cache metadata matches expected constraints.
     *
     * @param expected expected metadata constraints.
     * @return {@code true} if cache can be reused.
     */
    boolean matchesExpected(SchemaCacheMetadata expected) {
        if (expected == null) {
            return true;
        }
        var dialectMatches = Objects.equals(normalizeBlank(dialect), normalizeBlank(expected.dialect()));
        if (!dialectMatches) {
            return false;
        }
        var expectedProduct = normalizeBlank(expected.databaseProduct());
        if (expectedProduct != null && !Objects.equals(normalizeBlank(databaseProduct), expectedProduct)) {
            return false;
        }
        return expected.databaseMajorVersion() == null
            || Objects.equals(databaseMajorVersion, expected.databaseMajorVersion());
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

