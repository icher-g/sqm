package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Represents an ordered statement sequence containing top-level statements.
 * <p>
 * Empty statements are not represented in this model. Parser implementations
 * that accept repeated semicolons should skip those separators and populate
 * this container only with parsed statements.
 * </p>
 */
public non-sealed interface StatementSequence extends Node {

    /**
     * Creates an immutable statement sequence.
     *
     * @param statements ordered statements in the sequence
     * @return immutable statement sequence
     */
    static StatementSequence of(List<? extends Statement> statements) {
        return new Impl(List.copyOf(statements));
    }

    /**
     * Creates an immutable statement sequence from ordered statements.
     *
     * @param statements ordered statements in the sequence
     * @return immutable statement sequence
     */
    static StatementSequence of(Statement... statements) {
        return of(List.of(statements));
    }

    /**
     * Returns statements in source order.
     *
     * @return immutable ordered statement list
     */
    List<Statement> statements();

    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitStatementSequence(this);
    }

    /**
     * Default immutable implementation of {@link StatementSequence}.
     *
     * @param statements ordered statements in the sequence
     */
    record Impl(List<Statement> statements) implements StatementSequence {

        /**
         * Creates an immutable statement sequence implementation.
         *
         * @param statements ordered statements in the sequence
         */
        public Impl {
            Objects.requireNonNull(statements, "statements");
            statements = List.copyOf(statements);
        }
    }
}
