package io.sqm.core.walk;

import io.sqm.core.*;

/**
 * Visitor for typed hint nodes and hint arguments.
 *
 * @param <R> result type
 */
public interface HintVisitor<R> {

    /**
     * Visits a generic statement hint.
     *
     * @param hint statement hint
     * @return visitor result
     */
    R visitStatementHint(StatementHint hint);

    /**
     * Visits a generic table hint.
     *
     * @param hint table hint
     * @return visitor result
     */
    R visitTableHint(TableHint hint);

    /**
     * Visits an identifier-valued hint argument.
     *
     * @param arg hint argument
     * @return visitor result
     */
    R visitIdentifierHintArg(IdentifierHintArg arg);

    /**
     * Visits a qualified-name-valued hint argument.
     *
     * @param arg hint argument
     * @return visitor result
     */
    R visitQualifiedNameHintArg(QualifiedNameHintArg arg);

    /**
     * Visits an expression-valued hint argument.
     *
     * @param arg hint argument
     * @return visitor result
     */
    R visitExpressionHintArg(ExpressionHintArg arg);
}