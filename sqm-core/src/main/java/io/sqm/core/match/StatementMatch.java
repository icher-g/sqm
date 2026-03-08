package io.sqm.core.match;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link Statement} subtypes.
 *
 * @param <R> the result type produced by the match
 */
public interface StatementMatch<R> extends Match<Statement, R> {

    /**
     * Creates a new matcher for the given {@link Statement}.
     *
     * @param statement statement to match
     * @param <R> result type
     * @return a new matcher
     */
    static <R> StatementMatch<R> match(Statement statement) {
        return new StatementMatchImpl<>(statement);
    }

    /**
     * Registers a handler for {@link Query} statements.
     *
     * @param function query handler
     * @return {@code this} for fluent chaining
     */
    StatementMatch<R> query(Function<Query, R> function);

    /**
     * Registers a handler for {@link InsertStatement} statements.
     *
     * @param function insert handler
     * @return {@code this} for fluent chaining
     */
    StatementMatch<R> insert(Function<InsertStatement, R> function);

    /**
     * Registers a handler for {@link UpdateStatement} statements.
     *
     * @param function update handler
     * @return {@code this} for fluent chaining
     */
    StatementMatch<R> update(Function<UpdateStatement, R> function);

    /**
     * Registers a handler for {@link DeleteStatement} statements.
     *
     * @param function delete handler
     * @return {@code this} for fluent chaining
     */
    StatementMatch<R> delete(Function<DeleteStatement, R> function);
}
