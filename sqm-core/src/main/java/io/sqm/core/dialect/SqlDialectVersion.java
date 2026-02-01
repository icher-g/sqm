package io.sqm.core.dialect;

/**
 * Represents a dialect version for feature availability checks.
 * <p>
 * Versions are compared lexicographically by {@code major}, {@code minor},
 * and {@code patch} components.
 */
public final class SqlDialectVersion implements Comparable<SqlDialectVersion> {
    private static final SqlDialectVersion MINIMUM = new SqlDialectVersion(0, 0, 0);

    private final int major;
    private final int minor;
    private final int patch;

    private SqlDialectVersion(int major, int minor, int patch) {
        this.major = requireNonNegative(major, "major");
        this.minor = requireNonNegative(minor, "minor");
        this.patch = requireNonNegative(patch, "patch");
    }

    /**
     * Creates a dialect version with only a major component.
     *
     * @param major major version
     * @return a new {@link SqlDialectVersion}
     */
    public static SqlDialectVersion of(int major) {
        return new SqlDialectVersion(major, 0, 0);
    }

    /**
     * Creates a dialect version with major and minor components.
     *
     * @param major major version
     * @param minor minor version
     * @return a new {@link SqlDialectVersion}
     */
    public static SqlDialectVersion of(int major, int minor) {
        return new SqlDialectVersion(major, minor, 0);
    }

    /**
     * Creates a dialect version with major, minor, and patch components.
     *
     * @param major major version
     * @param minor minor version
     * @param patch patch version
     * @return a new {@link SqlDialectVersion}
     */
    public static SqlDialectVersion of(int major, int minor, int patch) {
        return new SqlDialectVersion(major, minor, patch);
    }

    /**
     * Returns the minimal supported version (0.0.0).
     *
     * @return minimal version instance
     */
    public static SqlDialectVersion minimum() {
        return MINIMUM;
    }

    /**
     * Gets the major version component.
     *
     * @return major version
     */
    public int major() {
        return major;
    }

    /**
     * Gets the minor version component.
     *
     * @return minor version
     */
    public int minor() {
        return minor;
    }

    /**
     * Gets the patch version component.
     *
     * @return patch version
     */
    public int patch() {
        return patch;
    }

    /**
     * Returns {@code true} if this version is at least the given version.
     *
     * @param other version to compare against
     * @return {@code true} when this version is greater than or equal to {@code other}
     */
    public boolean isAtLeast(SqlDialectVersion other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(SqlDialectVersion other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        int majorDiff = Integer.compare(major, other.major);
        if (majorDiff != 0) {
            return majorDiff;
        }
        int minorDiff = Integer.compare(minor, other.minor);
        if (minorDiff != 0) {
            return minorDiff;
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SqlDialectVersion that)) return false;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(major);
        result = 31 * result + Integer.hashCode(minor);
        result = 31 * result + Integer.hashCode(patch);
        return result;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
        return value;
    }
}
