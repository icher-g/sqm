package io.sqm.core.match;

import io.sqm.core.*;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base interface for pattern-style matching on SQM model nodes.
 * <p>
 * Implementations of this interface provide a fluent API for matching a specific
 * node subtype and applying a handler function to produce a result. The match is
 * evaluated lazily and finalized via one of the {@code otherwise(...)} terminal
 * operations or its convenience variants such as {@code orElse(...)}.
 *
 * @param <T> the node type being matched
 * @param <R> the result type returned by the matching operation
 */
public interface Match<T, R> {
    /**
     * Creates a new matcher for the given {@link Join}.
     *
     * @param j   the join to match on
     * @param <R> the result type
     * @return a new {@code JoinMatch} for {@code j}
     */
    static <R> JoinMatch<R> join(Join j) {
        return JoinMatch.match(j);
    }

    /**
     * Creates a new matcher for the given {@link Predicate}.
     *
     * @param p   the predicate to match on
     * @param <R> the result type
     * @return a new {@code PredicateMatch} for {@code p}
     */
    static <R> PredicateMatch<R> predicate(Predicate p) {
        return PredicateMatch.match(p);
    }

    /**
     * Creates a new matcher for the given {@link Query}.
     *
     * @param q   the query to match on
     * @param <R> the result type
     * @return a new {@code QueryMatch} for {@code q}
     */
    static <R> QueryMatch<R> query(Query q) {
        return QueryMatch.match(q);
    }

    /**
     * Creates a new matcher for the given {@link SelectItem}.
     *
     * @param i   the select item to match on
     * @param <R> the result type
     * @return a new {@code SelectItemMatch} for {@code i}
     */
    static <R> SelectItemMatch<R> selectItem(SelectItem i) {
        return SelectItemMatch.match(i);
    }

    /**
     * Creates a new matcher for the given {@link TableRef}.
     *
     * @param t   the table reference to match on
     * @param <R> the result type
     * @return a new {@code TableMatch} for {@code t}
     */
    static <R> TableRefMatch<R> tableRef(TableRef t) {
        return TableRefMatch.match(t);
    }

    /**
     * Creates a new matcher for the given {@link Expression}.
     *
     * @param e   the expression to match on (maybe any concrete {@code Expression} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code ExpressionMatch} for {@code e}
     */
    static <R> ExpressionMatch<R> expression(Expression e) {
        return ExpressionMatch.match(e);
    }

    /**
     * Creates a new matcher for the given {@link ParamExpr}.
     *
     * @param e   the expression to match on (maybe any concrete {@code ParamExpr} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code ParamsMatch} for {@code e}
     */
    static <R> ParamsMatch<R> param(ParamExpr e) {
        return ParamsMatch.match(e);
    }

    /**
     * Creates a new matcher for the given {@link ArithmeticExpr}.
     *
     * @param e   the expression to match on (maybe any concrete {@code ArithmeticExpr} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code ArithmeticMatch} for {@code e}
     */
    static <R> ArithmeticMatch<R> arithmetic(ArithmeticExpr e) {
        return ArithmeticMatch.match(e);
    }

    /**
     * Creates a new matcher for the given {@link FunctionExpr.Arg}.
     *
     * @param a   the function argument to match on (maybe any concrete {@code FunctionExpr.Arg} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code FunctionExprArgMatch} for {@code a}
     */
    static <R> FunctionExprArgMatch<R> funcArg(FunctionExpr.Arg a) {
        return new FunctionExprArgMatchImpl<>(a);
    }

    /**
     * Creates a new matcher for the given {@link ValueSet}.
     *
     * @param vs  the value set to match on (maybe any concrete {@code ValueSet} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code ValueSetMatch} for {@code vs}
     */
    static <R> ValueSetMatch<R> valueSet(ValueSet vs) {
        return ValueSetMatch.match(vs);
    }

    /**
     * Creates a new matcher for the given {@link BoundSpec}.
     *
     * @param bs  the bound spec to match on
     * @param <R> the result type
     * @return a new {@code BoundSpecMatch} for {@code fs}
     */
    static <R> BoundSpecMatch<R> boundSpec(BoundSpec bs) {
        return BoundSpecMatch.match(bs);
    }

    /**
     * Creates a new matcher for the given {@link FrameSpec}.
     *
     * @param fs  the frame spec to match on
     * @param <R> the result type
     * @return a new {@code FrameSpecMatch} for {@code fs}
     */
    static <R> FrameSpecMatch<R> frameSpec(FrameSpec fs) {
        return FrameSpecMatch.match(fs);
    }

    /**
     * Creates a new matcher for the given {@link OverSpec}.
     *
     * @param os  the over spec to match on
     * @param <R> the result type
     * @return a new {@code OverSpecMatch} for {@code os}
     */
    static <R> OverSpecMatch<R> overSpec(OverSpec os) {
        return OverSpecMatch.match(os);
    }

    /**
     * Throws any {@link Throwable} without declaring or wrapping it.
     * <p>
     * This "sneaky throw" helper is used by {@link #orElseThrow(Supplier)}
     * to rethrow checked exceptions in a type-safe way without polluting
     * the interface with {@code throws} declarations.
     *
     * @param t   the throwable to rethrow
     * @param <E> the compile-time type of the throwable
     * @param <R> the dummy return type to allow expression use
     * @return never returns normally; always throws the provided exception
     * @throws E the given throwable, rethrown as-is
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneakyThrow(Throwable t) throws E {
        throw (E) t; // never returns
    }

    /**
     * Terminal operation for this match chain.
     * <p>
     * Executes the first matching branch that was previously registered.
     * If none of the registered type handlers matched the input object,
     * the given fallback function will be applied.
     *
     * @param f a function providing a fallback value if no match occurred
     * @return the computed result, never {@code null} unless produced by the handler
     */
    R otherwise(Function<T, R> f);

    /**
     * Terminal operation that resolves to an empty {@link Optional}
     * if no match occurred, or an {@code Optional} containing the result otherwise.
     *
     * @return an {@code Optional} of the matched result
     */
    default Optional<R> otherwiseEmpty() {
        return Optional.ofNullable(otherwise(j -> null));
    }

    /**
     * Terminal operation that returns a default value if no match occurred.
     *
     * @param defaultValue the value to return if no branch matched
     * @return the matched result or the provided default value
     */
    default R orElse(R defaultValue) {
        return otherwise(j -> defaultValue);
    }

    /**
     * Terminal operation that obtains a default value from a {@link Supplier}
     * if no match occurred.
     *
     * @param s the supplier of a default value
     * @return the matched result or the supplier’s value
     */
    default R orElseGet(Supplier<R> s) {
        return otherwise(j -> s.get());
    }

    /**
     * Terminal operation that throws an exception if no match occurred.
     * <p>
     * This method uses {@link #sneakyThrow(Throwable)} internally to allow
     * rethrowing any checked exception without declaring it.
     *
     * @param ex  supplier that provides an exception to throw
     * @param <X> the type of the exception to throw
     * @return never returns normally
     */
    default <X extends Throwable> R orElseThrow(Supplier<X> ex) {
        return otherwise(j -> sneakyThrow(ex.get()));
    }
}

