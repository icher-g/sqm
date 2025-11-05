package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.io.Serializable;

/**
 * Represents a generic element of the SQM (Structured Query Model) tree.
 * <p>
 * All elements of a parsed or programmatically constructed SQL query—
 * such as expressions, predicates, table references, and query blocks—
 * implement this interface. It serves as the common root for the
 * entire model hierarchy.
 * </p>
 *
 * <p>
 * The {@link #accept(NodeVisitor)} method supports the
 * <em>Visitor Pattern</em>, allowing external operations
 * (such as traversal, transformation, validation, or rendering)
 * to be applied to nodes without modifying their internal structure.
 * </p>
 */
public sealed interface Node extends Serializable permits BoundSpec, CteDef, Expression, FrameSpec, FromItem, GroupBy, GroupItem, LimitOffset, OrderBy, OrderItem, OverSpec, PartitionBy, Query, SelectItem, WhenThen, WindowDef {
    /**
     * Accepts a {@link NodeVisitor} that performs an operation on this node.
     * <p>
     * Each concrete node class calls back into the visitor with a type-specific
     * {@code visitXxx(...)} method, allowing the visitor to handle each node
     * type appropriately.
     * </p>
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type produced by the visitor
     * @return the result of the visitor’s operation on this node,
     * or {@code null} if the visitor’s return type is {@link Void}
     */
    <R> R accept(NodeVisitor<R> v);
}
