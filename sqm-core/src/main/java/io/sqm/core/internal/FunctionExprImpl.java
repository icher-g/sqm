package io.sqm.core.internal;

import io.sqm.core.FunctionExpr;
import io.sqm.core.OrderBy;
import io.sqm.core.OverSpec;
import io.sqm.core.Predicate;

import java.util.List;

/**
 * Represents a function call.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     COUNT(*);
 *     UPPER(products.name)
 *     COUNT(DISTINCT t.id) AS c
 *     AVG(sales) FILTER (WHERE region = 'EU') OVER (PARTITION BY year)
 *     MODE() WITHIN GROUP (ORDER BY value)
 *     }
 * </pre>
 *
 * @param name        the name of the function.
 * @param args        a list of arguments. Can be NULL or empty if there are no arguments.
 * @param distinctArg indicates whether DISTINCT should be added before the list of arguments in the function call. {@code COUNT(DISTINCT t.id) AS c}.
 * @param withinGroup defines an ordered-set aggregates.
 * @param filter      a filter used to filter rows only for aggregates.
 * @param over        an OVER specification.
 */
public record FunctionExprImpl(String name, List<Arg> args, Boolean distinctArg, OrderBy withinGroup, Predicate filter, OverSpec over) implements FunctionExpr {
    /**
     * This constructor ensures the arguments list is immutable.
     *
     * @param name        the name of the function.
     * @param args        a list of arguments. Can be NULL or empty if there are no arguments.
     * @param distinctArg indicates whether DISTINCT should be added before the list of arguments in the function call. {@code COUNT(DISTINCT t.id) AS c}.
     * @param withinGroup defines an ordered-set aggregates.
     * @param filter      a filter used to filter rows only for aggregates.
     * @param over        an OVER specification.
     */
    public FunctionExprImpl {
        args = List.copyOf(args);
    }
}
