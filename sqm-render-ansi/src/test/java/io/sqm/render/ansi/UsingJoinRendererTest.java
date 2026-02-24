package io.sqm.render.ansi;

import io.sqm.core.JoinKind;
import io.sqm.core.TableRef;
import io.sqm.core.UsingJoin;
import io.sqm.core.Identifier;
import io.sqm.core.QuoteStyle;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsingJoinRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());
    private final UsingJoinRenderer renderer = new UsingJoinRenderer();

    @Test
    void rendersUsingColumnsList() {
        var join = UsingJoin.of(
            TableRef.table(Identifier.of("t")),
            JoinKind.INNER,
            List.of(Identifier.of("id"), Identifier.of("name"))
        );
        var w = new DefaultSqlWriter(ctx);
        renderer.render(join, ctx, w);
        assertEquals("USING (id, name)", w.toText(null).sql());
    }

    @Test
    void preservesOrFallsBackQuoteStyleForUsingColumns() {
        var join = UsingJoin.of(
            TableRef.table(Identifier.of("t")),
            JoinKind.INNER,
            List.of(
                Identifier.of("A", QuoteStyle.DOUBLE_QUOTE),
                Identifier.of("B", QuoteStyle.BACKTICK)
            )
        );
        var w = new DefaultSqlWriter(ctx);
        renderer.render(join, ctx, w);
        assertEquals("USING (\"A\", \"B\")", w.toText(null).sql());
    }
}
