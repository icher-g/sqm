package io.sqm.core.transform;

import io.sqm.core.*;
import io.sqm.core.walk.NodeVisitor;

/**
 * A base interface for transforming {@link Node} trees using the visitor pattern.
 * <p>
 * Implementations of this interface typically provide recursive logic for traversing
 * and optionally rewriting {@link Node} nodes. Each {@code visit*} method returns
 * either the same instance (if no change is required) or a newly constructed
 * {@link Node} representing the transformed result.
 * </p>
 *
 * <p>
 * The default {@link #transform(Node)} helper performs a null-safe entry point:
 * it dispatches to {@code expr.accept(this)} when the expression is non-null.
 * </p>
 *
 * <p>Example</p>
 * <pre>{@code
 * public final class RenameColumnTransformer implements NodeTransformer {
 *     @Override
 *     public Node visitColumnExpr(ColumnExpr c) {
 *         if ("u".equals(c.tableAlias()) && "id".equals(c.name())) {
 *             return ColumnExpr.of("u", "user_id");
 *         }
 *         return c;
 *     }
 * }
 *
 * Node transformed = new RenameColumnTransformer().transform(expr);
 * }</pre>
 *
 * @see NodeVisitor
 * @see Node
 */
public interface NodeTransformer extends NodeVisitor<Node> {
    /**
     * Transforms the given {@link Node} node using this transformer.
     * If the node is {@code null}, {@code null} is returned.
     *
     * @param n the node to transform, may be {@code null}
     * @return the transformed node or {@code null}
     */
    default Node transform(Node n) {
        return (n == null) ? null : n.accept(this);
    }
}

