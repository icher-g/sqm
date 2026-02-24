package io.sqm.render.ansi;

import io.sqm.core.*;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowIdentifierRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    void renders_over_ref_with_preserved_or_fallback_quote_style() {
        assertEquals("\"W\"", ctx.render(OverSpec.ref(Identifier.of("W", QuoteStyle.DOUBLE_QUOTE))).sql());
        assertEquals("\"W\"", ctx.render(OverSpec.ref(Identifier.of("W", QuoteStyle.BACKTICK))).sql());
    }

    @Test
    void renders_over_def_base_window_with_preserved_or_fallback_quote_style() {
        var orderBy = OrderBy.of(OrderItem.of(col("v")));
        assertEquals("\"W\" ORDER BY v",
            ctx.render(OverSpec.def(Identifier.of("W", QuoteStyle.DOUBLE_QUOTE), orderBy, null, null)).sql());
        assertEquals("\"W\" ORDER BY v",
            ctx.render(OverSpec.def(Identifier.of("W", QuoteStyle.BACKTICK), orderBy, null, null)).sql());
    }

    @Test
    void renders_window_def_name_with_preserved_or_fallback_quote_style() {
        var spec = OverSpec.def((io.sqm.core.PartitionBy) null, OrderBy.of(OrderItem.of(col("v"))), null, null);
        assertEquals("WINDOW \"W\" AS (ORDER BY v)",
            ctx.render(WindowDef.of(Identifier.of("W", QuoteStyle.DOUBLE_QUOTE), spec)).sql());
        assertEquals("WINDOW \"W\" AS (ORDER BY v)",
            ctx.render(WindowDef.of(Identifier.of("W", QuoteStyle.BACKTICK), spec)).sql());
    }
}

