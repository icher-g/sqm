package io.sqm.render;

import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryRendererTest {

    @Test
    void delegatesToConcreteQueryRenderer() {
        var dialect = new RenderTestDialect()
            .register(new QueryRenderer())
            .register(new SelectRenderer());
        var ctx = RenderContext.of(dialect);

        Query query = select(lit(1)).build();
        var sql = ctx.render(query).sql();

        assertEquals("SELECT-STUB", sql);
    }

    private static final class SelectRenderer implements Renderer<SelectQuery> {
        @Override
        public void render(SelectQuery node, RenderContext ctx, SqlWriter w) {
            w.append("SELECT-STUB");
        }

        @Override
        public Class<SelectQuery> targetType() {
            return SelectQuery.class;
        }
    }
}
