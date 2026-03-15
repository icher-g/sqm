package io.sqm.render.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.core.OverSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders scalar function expressions.
 */
@SuppressWarnings("unused")
public class FunctionExprRenderer implements Renderer<FunctionExpr> {
    /**
     * Creates a function-expression renderer.
     */
    public FunctionExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        renderFunctionName(node, ctx, w);
        renderOpenParen(node, ctx, w);
        renderDistinct(node, ctx, w);
        renderArguments(node, ctx, w);
        renderCloseParen(node, ctx, w);
        renderWithinGroup(node, ctx, w);
        renderFilter(node, ctx, w);
        renderOver(node, ctx, w);
    }

    /**
     * Renders the function name.
     *
     * @param node function expression to render.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderFunctionName(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        w.append(renderQualifiedName(node.name(), ctx.dialect().quoter()));
    }

    /**
     * Renders the opening parenthesis before function arguments.
     *
     * @param node function expression to render.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderOpenParen(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        w.append("(");
    }

    /**
     * Renders the optional {@code DISTINCT} marker before function arguments.
     *
     * @param node function expression to render.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderDistinct(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        if (node.distinctArg() != null && node.distinctArg()) {
            w.append("DISTINCT");
            if (!node.args().isEmpty()) {
                w.space();
            }
        }
    }

    /**
     * Renders the function argument list.
     *
     * @param node function expression to render.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderArguments(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        for (int i = 0; i < node.args().size(); i++) {
            if (i > 0) {
                w.append(", ");
            }
            renderArgument(node, node.args().get(i), i, ctx, w);
        }
    }

    /**
     * Renders one function argument.
     *
     * @param node function expression to render.
     * @param arg current argument.
     * @param index zero-based argument index.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderArgument(
        FunctionExpr node,
        FunctionExpr.Arg arg,
        int index,
        RenderContext ctx,
        SqlWriter w
    ) {
        w.append(arg);
    }

    /**
     * Renders the closing parenthesis after function arguments.
     *
     * @param node function expression to render.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderCloseParen(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        w.append(")");
    }

    /**
     * Renders the optional {@code WITHIN GROUP (...)} clause.
     *
     * @param node function expression to render.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderWithinGroup(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        if (node.withinGroup() != null) {
            w.space().append("WITHIN GROUP").space().append("(");
            w.append(node.withinGroup());
            w.append(")");
        }
    }

    /**
     * Renders the optional {@code FILTER (WHERE ...)} clause.
     *
     * @param node function expression to render.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderFilter(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        if (node.filter() != null) {
            w.space().append("FILTER").space().append("(");
            w.append("WHERE").space();
            w.append(node.filter());
            w.append(")");
        }
    }

    /**
     * Renders the optional {@code OVER ...} clause.
     *
     * @param node function expression to render.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderOver(FunctionExpr node, RenderContext ctx, SqlWriter w) {
        if (node.over() != null) {
            w.space().append("OVER").space();
            w.append(node.over(), node.over() instanceof OverSpec.Def, false);
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr> targetType() {
        return FunctionExpr.class;
    }
}
