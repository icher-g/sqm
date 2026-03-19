package io.sqm.core.walk;

import io.sqm.core.Assignment;
import io.sqm.core.CteDef;
import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.LockingClause;
import io.sqm.core.ResultClause;
import io.sqm.core.ResultInto;
import io.sqm.core.TypeName;
import io.sqm.core.UpdateStatement;
import io.sqm.core.WhenThen;

/**
 * Visitor for statement-level SQM nodes.
 *
 * @param <R> the result type produced by the visitor
 */
public interface StatementVisitor<R> {

    /**
     * Visits a dialect-neutral {@link InsertStatement}.
     *
     * @param statement insert statement being visited
     * @return a result specific to the visitor implementation
     */
    R visitInsertStatement(InsertStatement statement);

    /**
     * Visits a dialect-neutral {@link UpdateStatement}.
     *
     * @param statement update statement being visited
     * @return a result specific to the visitor implementation
     */
    R visitUpdateStatement(UpdateStatement statement);

    /**
     * Visits a dialect-neutral {@link DeleteStatement}.
     *
     * @param statement delete statement being visited
     * @return a result specific to the visitor implementation
     */
    R visitDeleteStatement(DeleteStatement statement);

    /**
     * Visits a SQL Server DML {@link ResultClause}.
     *
     * @param clause result clause being visited
     * @return a result specific to the visitor implementation
     */
    R visitResultClause(ResultClause clause);

    /**
     * Visits a SQL Server {@link ResultInto} target.
     *
     * @param into result-into target being visited
     * @return a result specific to the visitor implementation
     */
    R visitResultInto(ResultInto into);

    /**
     * Visits a single {@link Assignment} within an update statement.
     *
     * @param assignment assignment being visited
     * @return a result specific to the visitor implementation
     */
    R visitAssignment(Assignment assignment);

    /**
     * Visits a single {@link WhenThen} clause.
     *
     * @param w the {@link WhenThen} clause being visited
     * @return a result specific to the visitor implementation
     */
    R visitWhenThen(WhenThen w);

    /**
     * Visits a {@link CteDef} node.
     *
     * @param c the CTE definition being visited
     * @return a result specific to the visitor implementation
     */
    R visitCte(CteDef c);

    /**
     * Visits a {@link TypeName}.
     *
     * @param typeName the type name node
     * @return visitor result
     */
    R visitTypeName(TypeName typeName);

    /**
     * Visits a PostgreSQL SELECT locking clause.
     *
     * @param clause locking clause node
     * @return result produced by the visitor
     */
    R visitLockingClause(LockingClause clause);
}
