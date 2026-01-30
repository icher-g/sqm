package io.sqm.render.repos;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import io.sqm.render.spi.RenderersRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRenderersRepositoryTest {

    @Test
    void registerAndRetrieveRenderers() {
        RenderersRepository repo = new DefaultRenderersRepository();
        var renderer = new ColumnRenderer();

        repo.register(renderer);

        assertSame(renderer, repo.get(ColumnExpr.class));
        assertSame(renderer, repo.getFor(ColumnExpr.of("a")));
        assertSame(renderer, repo.require(ColumnExpr.class));
        assertSame(renderer, repo.requireFor(ColumnExpr.of("b")));
    }

    @Test
    void requireThrowsWhenMissing() {
        RenderersRepository repo = new DefaultRenderersRepository();

        var ex = assertThrows(IllegalArgumentException.class, () -> repo.require(Expression.class));
        assertTrue(ex.getMessage().contains("No handler registered"));
    }

    private static final class ColumnRenderer implements Renderer<ColumnExpr> {
        @Override
        public void render(ColumnExpr node, RenderContext ctx, SqlWriter w) {
            w.append(node.name());
        }

        @Override
        public Class<ColumnExpr> targetType() {
            return ColumnExpr.class;
        }
    }
}
