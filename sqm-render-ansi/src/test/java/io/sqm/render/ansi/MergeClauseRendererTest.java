package io.sqm.render.ansi;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeUpdateAction;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MergeClauseRendererTest {

    private static String render(SupportedMergeClauseRenderer renderer, MergeClause clause, RenderContext ctx) {
        var writer = new DefaultSqlWriter(ctx);
        renderer.render(clause, ctx, writer);
        return writer.toText(java.util.List.of()).sql().replaceAll("\\s+", " ").trim();
    }

    @Test
    void rejectsMergeClauseByDefault() {
        var renderer = new MergeClauseRenderer();
        var ctx = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> renderer.render(MergeClause.of(MergeClause.MatchType.MATCHED, MergeDeleteAction.of()), ctx, writer)
        );
    }

    @Test
    void rendersSharedMergeClauseShapesThroughSupportedHook() {
        var renderer = new SupportedMergeClauseRenderer();
        var ctx = RenderContext.of(new SupportedMergeClauseDialect());

        var matched = render(renderer, MergeClause.of(MergeClause.MatchType.MATCHED, MergeDeleteAction.of()), ctx);
        var notMatched = render(
            renderer,
            MergeClause.of(
                MergeClause.MatchType.NOT_MATCHED,
                col("src", "id").gt(lit(0)),
                MergeInsertAction.of(java.util.List.of(id("id")), row(col("src", "id")))
            ),
            ctx
        );
        var bySource = render(
            renderer,
            MergeClause.of(
                MergeClause.MatchType.NOT_MATCHED_BY_SOURCE,
                col("users", "active").eq(lit(true)),
                MergeUpdateAction.of(java.util.List.of(set("name", lit("archived"))))
            ),
            ctx
        );

        assertEquals("WHEN MATCHED THEN DELETE", matched);
        assertEquals("WHEN NOT MATCHED AND src.id > 0 THEN INSERT (id) VALUES src.id", notMatched);
        assertEquals("WHEN NOT MATCHED BY SOURCE AND users.active = TRUE THEN UPDATE SET name = 'archived'", bySource);
    }

    @Test
    void exposesMergeClauseTargetType() {
        assertEquals(MergeClause.class, new MergeClauseRenderer().targetType());
    }

    private static final class SupportedMergeClauseRenderer extends MergeClauseRenderer {
        @Override
        public void render(MergeClause node, RenderContext ctx, SqlWriter w) {
            renderSupportedClause(node, w);
        }
    }

    private static final class SupportedMergeClauseDialect implements SqlDialect {
        private final AnsiDialect delegate = new AnsiDialect();
        private final RenderersRepository renderers = Renderers.ansiCopy()
            .register(new SupportedMergeDeleteActionRenderer())
            .register(new SupportedMergeUpdateActionRenderer())
            .register(new SupportedMergeInsertActionRenderer());

        @Override
        public io.sqm.render.spi.PreparedNode beforeRender(io.sqm.core.Node root, RenderOptions options) {
            return delegate.beforeRender(root, options);
        }

        @Override
        public String name() {
            return delegate.name();
        }

        @Override
        public IdentifierQuoter quoter() {
            return delegate.quoter();
        }

        @Override
        public ValueFormatter formatter() {
            return delegate.formatter();
        }

        @Override
        public Operators operators() {
            return delegate.operators();
        }

        @Override
        public Booleans booleans() {
            return delegate.booleans();
        }

        @Override
        public NullSorting nullSorting() {
            return delegate.nullSorting();
        }

        @Override
        public PaginationStyle paginationStyle() {
            return delegate.paginationStyle();
        }

        @Override
        public DialectCapabilities capabilities() {
            return delegate.capabilities();
        }

        @Override
        public RenderersRepository renderers() {
            return renderers;
        }
    }

    private static final class SupportedMergeDeleteActionRenderer implements Renderer<MergeDeleteAction> {
        @Override
        public void render(MergeDeleteAction node, RenderContext ctx, SqlWriter w) {
            w.append("DELETE");
        }

        @Override
        public Class<MergeDeleteAction> targetType() {
            return MergeDeleteAction.class;
        }
    }

    private static final class SupportedMergeUpdateActionRenderer implements Renderer<MergeUpdateAction> {
        @Override
        public void render(MergeUpdateAction node, RenderContext ctx, SqlWriter w) {
            w.append("UPDATE").space().append("SET").space().comma(node.assignments());
        }

        @Override
        public Class<MergeUpdateAction> targetType() {
            return MergeUpdateAction.class;
        }
    }

    private static final class SupportedMergeInsertActionRenderer implements Renderer<MergeInsertAction> {
        @Override
        public void render(MergeInsertAction node, RenderContext ctx, SqlWriter w) {
            w.append("INSERT");
            if (!node.columns().isEmpty()) {
                w.space().append("(").comma(node.columns(), ctx.dialect().quoter()).append(")");
            }
            w.space().append("VALUES").space().append(node.values());
        }

        @Override
        public Class<MergeInsertAction> targetType() {
            return MergeInsertAction.class;
        }
    }
}
