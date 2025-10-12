package io.cherlabs.sqm.lang;

/**
 * A helper class to return three results from a function.
 *
 * @param first  a first item.
 * @param second a second item.
 * @param third  a third item.
 * @param <T1>   a first item type.
 * @param <T2>   a second item type.
 * @param <T3>   a third item type.
 */
public record Tuple3<T1, T2, T3>(T1 first, T2 second, T3 third) {
}
