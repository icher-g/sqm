package io.sqm.core;

/**
 * A base expression type representing a bind parameter in a SQL statement.
 * <p>
 * SQM supports three kinds of parameters:
 * <ul>
 *   <li>{@link AnonymousParamExpr} – an unindexed positional parameter,
 *       represented in SQL as {@code ?} and assigned a position based on
 *       appearance order.</li>
 *
 *   <li>{@link OrdinalParamExpr} – a positional parameter with an explicit
 *       index, such as {@code $1} (PostgreSQL) or {@code ?1} (JPQL).</li>
 *
 *   <li>{@link NamedParamExpr} – a parameter identified by name, such as
 *       {@code :id}, {@code @id}, or template-style parameters.</li>
 * </ul>
 * <p>
 * While different SQL dialects expose different parameter syntaxes,
 * {@code ParamExpr} models only the semantic category. The renderer for each
 * dialect decides how the parameter should be emitted in the final SQL.
 */
public sealed interface ParamExpr extends Expression permits AnonymousParamExpr, NamedParamExpr, OrdinalParamExpr {

    /**
     * Creates a new {@link AnonymousParamExpr} with the given logical position.
     *
     * @return an anonymous positional parameter
     */
    static AnonymousParamExpr anonymous() {
        return AnonymousParamExpr.of();
    }

    /**
     * Creates a new {@link NamedParamExpr} for the given parameter name.
     * <p>
     * The name should not contain any dialect-specific prefix such as {@code :},
     * {@code @}, or {@code #{}}; it must be the canonical name only.
     *
     * @param name canonical parameter name (e.g. {@code "userId"})
     * @return a named parameter
     */
    static NamedParamExpr named(String name) {
        return NamedParamExpr.of(name);
    }

    /**
     * Creates a new {@link OrdinalParamExpr} with the given explicit index.
     * <p>
     * For example, an index of {@code 1} corresponds to parameters like
     * {@code $1} (PostgreSQL) or {@code ?1} (JPQL).
     *
     * @param index the 1-based explicit index of the parameter
     * @return an ordinal parameter
     */
    static OrdinalParamExpr ordinal(int index) {
        return OrdinalParamExpr.of(index);
    }
}

