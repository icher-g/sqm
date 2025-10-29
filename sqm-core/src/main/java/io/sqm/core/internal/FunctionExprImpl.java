package io.sqm.core.internal;

import io.sqm.core.FunctionExpr;

import java.util.List;

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
 *
 * @param name        the name of the function.
 * @param args        a list of arguments. Can be NULL or empty if there are no arguments.
 * @param distinctArg indicates whether DISTINCT should be added before the list of arguments in the function call. {@code COUNT(DISTINCT t.id) AS c}.
 */
public record FunctionExprImpl(String name, List<Arg> args, Boolean distinctArg) implements FunctionExpr {
}
