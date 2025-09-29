package io.cherlabs.sqlmodel.core;

import java.util.List;

/**
 * An interface representing a base for all column statements.
 */
public interface Column extends Entity {
    /**
     * Creates a column that has name/table/alias.
     * For example:
     * {@code
     * Sales.ID AS SID
     * }
     *
     * @param name the name of the column.
     * @return A newly created instance of the column.
     */
    static NamedColumn of(String name) {
        return new NamedColumn(name, null, null);
    }

    /**
     * Creates a column that is represented by a sub query.
     * For example:
     * {@code
     * SELECT ID FROM Sales
     * }
     *
     * @param query a sub query.
     * @return A newly created instance of a column.
     */
    static QueryColumn of(Query<?> query) {
        return new QueryColumn(query, null);
    }

    /**
     * Creates a column that represents a CASE statement.
     * For example:
     * {@code
     * CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END AS result
     * }
     *
     * @param when a WHEN...THEN statement to start with.
     * @return A newly created instance of a column.
     */
    static CaseColumn of(WhenThen when) {
        return CaseColumn.of(when);
    }

    /**
     * Creates a column that represents a function call.
     * For example:
     * {@code
     * concat('Hello', ', ', 'World') AS greeting
     * }
     *
     * @param name a name of the function.
     * @param args an array of function arguments.
     * @return A newly created instance of a column.
     */
    static FunctionColumn func(String name, FunctionColumn.Arg... args) {
        return new FunctionColumn(name, List.of(args), false, null);
    }

    /**
     * Creates a column that is represented by a free expr. This should be used only if there is no other implementation
     * that can support the requirement.
     *
     * @param exp an expr.
     * @return A newly created instance of a column.
     */
    static ExpressionColumn expr(String exp) {
        return new ExpressionColumn(exp, null);
    }
}
