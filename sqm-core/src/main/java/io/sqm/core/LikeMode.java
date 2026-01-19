package io.sqm.core;

/**
 * Specifies the pattern matching operator used by {@link LikePredicate}.
 *
 * <p>This enum models SQL pattern matching operators that compare a value
 * against a pattern expression. Support for individual modes depends on
 * the SQL dialect used during rendering.</p>
 *
 * <ul>
 *   <li>{@link #LIKE} – Standard SQL pattern matching using '%' and '_' wildcards.</li>
 *   <li>{@link #ILIKE} – PostgreSQL-specific case-insensitive variant of {@code LIKE}.</li>
 *   <li>{@link #SIMILAR_TO} – PostgreSQL pattern matching operator based on
 *       SQL regular expression syntax.</li>
 * </ul>
 *
 * <p>Negation (e.g. {@code NOT LIKE}, {@code NOT ILIKE}, {@code NOT SIMILAR TO})
 * is modeled separately via {@link LikePredicate#negated()}.</p>
 *
 * <p>The optional {@code ESCAPE} clause is supported for all modes where
 * the target SQL dialect allows it.</p>
 *
 * @see LikePredicate
 */
public enum LikeMode {

    /**
     * Standard SQL {@code LIKE} operator.
     *
     * <p>Performs case-sensitive pattern matching using
     * {@code %} (multi-character) and {@code _} (single-character) wildcards.</p>
     *
     * <p>This mode is supported by all SQL dialects.</p>
     */
    LIKE,

    /**
     * PostgreSQL {@code ILIKE} operator.
     *
     * <p>Performs case-insensitive pattern matching using the same wildcard
     * semantics as {@code LIKE}.</p>
     *
     * <p>This operator is PostgreSQL-specific and not part of ANSI SQL.</p>
     */
    ILIKE,

    /**
     * PostgreSQL {@code SIMILAR TO} operator.
     *
     * <p>Performs pattern matching using SQL regular expression syntax,
     * combining features of {@code LIKE} and POSIX-style regular expressions.</p>
     *
     * <p>Although defined in the SQL standard, support outside PostgreSQL
     * is limited and dialect-dependent.</p>
     */
    SIMILAR_TO
}

