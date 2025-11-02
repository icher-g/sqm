package io.sqm.core.match;

import java.util.Optional;
import java.util.function.Function;

/**
 * A small utility class that provides a fluent, type-safe way to chain
 * operations that return {@link Optional} values without deeply nesting
 * {@code flatMap} calls.
 * <p>
 * Example usage:
 * <pre>{@code
 * var name = Opts.start(query)
 *     .then(Query::asSelect)
 *     .then(s -> s.joins().stream().findFirst())
 *     .then(Join::asOn)
 *     .then(on -> on.on().asComparison())
 *     .then(cmp -> cmp.lhs().asColumn())
 *     .map(ColumnExpr::name);
 *
 * name.ifPresent(System.out::println);
 * }</pre>
 * This class is especially useful for safely traversing hierarchical or
 * polymorphic models, such as AST or SQL model trees, where intermediate
 * nodes may be absent.
 */
public final class Opts {

    /**
     * Utility class — not meant to be instantiated.
     */
    private Opts() {
    }

    /**
     * Starts a new {@link Chain} with an initial value.
     * <p>
     * The value may be {@code null}; in that case, the resulting chain will
     * represent an empty {@link Optional}.
     *
     * @param value the initial value (may be {@code null})
     * @param <T>   the type of the initial value
     * @return a new {@code Chain} initialized with the given value
     */
    public static <T> Chain<T> start(T value) {
        return new ChainImpl<>(Optional.ofNullable(value));
    }

    /**
     * Represents a fluent chain of {@link Optional}-aware transformations.
     * <p>
     * Each step can either return another {@code Optional} value via
     * {@link #then(Function)}, or a direct mapping via {@link #thenMap(Function)}.
     * The chain can be terminated with {@link #toOptional()} or a final
     * mapping using {@link #map(Function)}.
     *
     * @param <T> the type of the value currently held in the chain
     */
    public interface Chain<T> {

        /**
         * Applies a transformation that itself produces an {@link Optional}.
         * <p>
         * If the current chain is empty, the function is not invoked.
         * Equivalent to {@link Optional#flatMap(Function)} but returns another
         * {@code Chain} for continued fluent use.
         *
         * @param f   a mapping function from {@code T} to {@code Optional<U>}
         * @param <U> the result type of the mapping function
         * @return a new {@code Chain} holding the result of the mapping
         */
        <U> Chain<U> then(Function<? super T, Optional<U>> f);

        /**
         * Applies a transformation that produces a plain value (not wrapped
         * in an {@link Optional}).
         * <p>
         * If the current chain is empty, the function is not invoked.
         * Equivalent to {@link Optional#map(Function)} but returns another
         * {@code Chain} for continued fluent use.
         *
         * @param f   a mapping function from {@code T} to {@code U}
         * @param <U> the result type of the mapping function
         * @return a new {@code Chain} holding the mapped value
         */
        <U> Chain<U> thenMap(Function<? super T, ? extends U> f);

        /**
         * Returns the underlying {@link Optional} representing the result
         * of the chain.
         *
         * @return the current {@code Optional} value
         */
        Optional<T> toOptional();

        /**
         * Applies a terminal mapping function to the final value of this
         * chain, returning the result wrapped in an {@link Optional}.
         * <p>
         * This is a convenience shortcut for
         * {@code chain.toOptional().map(f)}.
         *
         * @param f   a mapping function from {@code T} to {@code U}
         * @param <U> the result type of the mapping function
         * @return an {@code Optional} containing the mapped result, or empty
         * if the chain was empty
         */
        default <U> Optional<U> map(Function<? super T, ? extends U> f) {
            return toOptional().map(f);
        }
    }

    /**
     * Default {@link Chain} implementation backed by an {@link Optional}.
     * <p>
     * Each transformation produces a new {@code ChainImpl} that wraps the
     * transformed {@code Optional}.
     *
     * @param <T> the type of value wrapped by this chain
     */
    private record ChainImpl<T>(Optional<T> opt) implements Chain<T> {

        @Override
        public <U> Chain<U> then(Function<? super T, Optional<U>> f) {
            return new ChainImpl<>(opt.flatMap(f));
        }

        @Override
        public <U> Chain<U> thenMap(Function<? super T, ? extends U> f) {
            return new ChainImpl<>(opt.map(f));
        }

        @Override
        public Optional<T> toOptional() {
            return opt;
        }
    }
}


