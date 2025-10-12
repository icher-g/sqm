package io.cherlabs.sqm.lang;

/**
 * A helper class to return two results from a function.
 *
 * @param first  a first item.
 * @param second a second item.
 * @param <T1>   a first item type.
 * @param <T2>   a second item type.
 */
public record Tuple2<T1, T2>(T1 first, T2 second) {
}
