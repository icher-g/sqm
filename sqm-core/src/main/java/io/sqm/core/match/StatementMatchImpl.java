package io.sqm.core.match;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link Statement}.
 *
 * @param <R> result type
 */
public final class StatementMatchImpl<R> implements StatementMatch<R> {

    private final Statement statement;
    private boolean matched;
    private R result;

    /**
     * Creates a matcher for the provided statement.
     *
     * @param statement statement to match
     */
    public StatementMatchImpl(Statement statement) {
        this.statement = statement;
    }

    @Override
    public StatementMatch<R> query(Function<Query, R> function) {
        if (!matched && statement instanceof Query query) {
            result = function.apply(query);
            matched = true;
        }
        return this;
    }

    @Override
    public StatementMatch<R> insert(Function<InsertStatement, R> function) {
        if (!matched && statement instanceof InsertStatement insertStatement) {
            result = function.apply(insertStatement);
            matched = true;
        }
        return this;
    }

    @Override
    public StatementMatch<R> update(Function<UpdateStatement, R> function) {
        if (!matched && statement instanceof UpdateStatement updateStatement) {
            result = function.apply(updateStatement);
            matched = true;
        }
        return this;
    }

    @Override
    public StatementMatch<R> delete(Function<DeleteStatement, R> function) {
        if (!matched && statement instanceof DeleteStatement deleteStatement) {
            result = function.apply(deleteStatement);
            matched = true;
        }
        return this;
    }

    @Override
    public R otherwise(Function<Statement, R> function) {
        return matched ? result : function.apply(statement);
    }
}
