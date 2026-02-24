package io.sqm.core;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Represents a multipart SQL identifier name, such as {@code pg_catalog.int4}.
 *
 * <p>Each part preserves its quote style via {@link Identifier}.</p>
 *
 * @param parts identifier parts in source order
 */
public record QualifiedName(List<Identifier> parts) implements Serializable {
    /**
     * Creates a qualified name.
     *
     * @param parts identifier parts in source order
     */
    public QualifiedName {
        Objects.requireNonNull(parts, "parts");
        parts = List.copyOf(parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("parts must not be empty");
        }
        for (var part : parts) {
            Objects.requireNonNull(part, "parts contains null");
        }
    }

    /**
     * Creates a qualified name from string parts using default quote style.
     *
     * @param parts identifier values in source order
     * @return a qualified name
     */
    public static QualifiedName of(List<String> parts) {
        Objects.requireNonNull(parts, "parts");
        return new QualifiedName(parts.stream().map(Identifier::of).toList());
    }

    /**
     * Creates a qualified name from string parts using default quote style.
     *
     * @param parts identifier values in source order
     * @return a qualified name
     */
    public static QualifiedName of(String... parts) {
        return of(List.of(parts));
    }

    /**
     * Creates a qualified name from identifier parts.
     *
     * @param parts identifier parts in source order
     * @return a qualified name
     */
    public static QualifiedName of(Identifier... parts) {
        return new QualifiedName(List.of(parts));
    }

    /**
     * Returns identifier values without quote metadata.
     *
     * @return plain identifier values in source order
     */
    public List<String> values() {
        return parts.stream().map(Identifier::value).toList();
    }
}
