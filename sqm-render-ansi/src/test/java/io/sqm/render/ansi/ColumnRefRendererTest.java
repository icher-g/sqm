package io.sqm.render.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Identifier;
import io.sqm.core.QuoteStyle;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnRefRendererTest {
    private static String render(ColumnExpr expr) {
        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);
        w.append(expr);
        return w.toText(List.of()).sql();
    }

    @Test
    void preserves_double_quotes_and_converts_unsupported_quote_styles() {
        var expr = ColumnExpr.of(
            Identifier.of("U", QuoteStyle.BRACKETS),
            Identifier.of("Name", QuoteStyle.DOUBLE_QUOTE)
        );
        assertEquals("\"U\".\"Name\"", render(expr));
    }
}

