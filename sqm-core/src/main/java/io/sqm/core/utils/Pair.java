package io.sqm.core.utils;

/**
 * A generic immutable pair of two values.
 * <p>
 * This record is a simple value carrier that groups two related values
 * without introducing domain-specific semantics. It is primarily intended
 * for local, internal use where defining a dedicated type would be excessive.
 *
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 * @param first the first value
 * @param second the second value
 */
public record Pair<A, B>(A first, B second) {}

