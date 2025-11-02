package io.sqm.core;

import io.sqm.core.internal.*;
import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Represents a function call.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     COUNT(*);
 *     UPPER(products.name)
 *     COUNT(DISTINCT t.id) AS c
 *     }
 * </pre>
 */
public non-sealed interface FunctionExpr extends Expression {
    /**
     * Creates a function call expression.
     *
     * @param name a function name
     * @param args an array of function arguments.
     * @return A newly created instance of a function call expression.
     */
    static FunctionExpr of(String name, FunctionExpr.Arg... args) {
        return new FunctionExprImpl(Objects.requireNonNull(name), List.of(args), null);
    }

    /**
     * Creates a function call expression.
     *
     * @param name        a function name
     * @param distinctArg indicates whether DISTINCT should be added before the list of arguments in the function call. {@code COUNT(DISTINCT t.id) AS c}
     * @param args        an array of function arguments.
     * @return A newly created instance of a function call expression.
     */
    static FunctionExpr of(String name, Boolean distinctArg, FunctionExpr.Arg... args) {
        return new FunctionExprImpl(Objects.requireNonNull(name), List.of(args), distinctArg);
    }

    /**
     * Creates a function call expression.
     *
     * @param name        a function name
     * @param distinctArg indicates whether DISTINCT should be added before the list of arguments in the function call. {@code COUNT(DISTINCT t.id) AS c}
     * @param args        an array of function arguments.
     * @return A newly created instance of a function call expression.
     */
    static FunctionExpr of(String name, Boolean distinctArg, List<FunctionExpr.Arg> args) {
        return new FunctionExprImpl(Objects.requireNonNull(name), args, distinctArg);
    }

    /**
     * Gets the name of the function.
     *
     * @return the name of the function.
     */
    String name();

    /**
     * Gets a list of arguments. Can be NULL or empty if there are no arguments.
     *
     * @return a list of arguments if exists or NULL otherwise.
     */
    List<Arg> args();

    /**
     * Indicates whether DISTINCT should be added before the list of arguments in the function call.
     * {@code COUNT(DISTINCT t.id) AS c}
     *
     * @return True if DISTINCT needs to be added and False otherwise.
     */
    Boolean distinctArg();

    /**
     * Adds DISTINCT indication to an expression.
     *
     * @param distinctArg an indication.
     * @return A newly created instance of the function call expression with the added indication. All other fields are preserved.
     */
    default FunctionExpr distinct(boolean distinctArg) {
        return new FunctionExprImpl(name(), args(), distinctArg);
    }

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitFunctionExpr(this);
    }

    /* -------- Function arguments (structured) -------- */
    sealed interface Arg extends Expression permits FunctionExpr.Arg.Column, FunctionExpr.Arg.Literal, FunctionExpr.Arg.Function, FunctionExpr.Arg.Star {
        /**
         * Creates column function argument.
         *
         * @param c a column reference.
         * @return a column argument.
         */
        static Column column(ColumnExpr c) {
            return new FuncColumnArg(c);
        }

        /**
         * Creates literal function argument.
         *
         * @param v a literal value.
         * @return a literal argument.
         */
        static Literal literal(Object v) {
            return new FuncLiteralArg(v);
        }

        /**
         * Create function call argument.
         *
         * @param f a nested function call.
         * @return a function call argument.
         */
        static Function func(FunctionExpr f) {
            return new FuncCallArg(f);
        }

        /**
         * Creates a '*' argument.
         *
         * @return a star argument.
         */
        static Star star() {
            return new FuncStarArg();
        }

        /**
         * Accepts a {@link NodeVisitor} and dispatches control to the
         * visitor method corresponding to the concrete subtype
         *
         * @param v   the visitor instance to accept (must not be {@code null})
         * @param <R> the result type returned by the visitor
         * @return the result produced by the visitor
         */
        @Override
        default <R> R accept(NodeVisitor<R> v) {
            return v.visitFunctionArgExpr(this);
        }

        /**
         * Column reference argument: t.c or just c.
         */
        non-sealed interface Column extends FunctionExpr.Arg {
            ColumnExpr ref();
        }

        /**
         * Literal argument (String, Number, Boolean, null).
         */
        non-sealed interface Literal extends FunctionExpr.Arg {
            Object value();
        }

        /**
         * Nested function call as an argument.
         */
        non-sealed interface Function extends FunctionExpr.Arg {
            FunctionExpr call();
        }

        /**
         * The '*' argument (e.g., COUNT(*))
         */
        non-sealed interface Star extends FunctionExpr.Arg {
        }
    }
}
