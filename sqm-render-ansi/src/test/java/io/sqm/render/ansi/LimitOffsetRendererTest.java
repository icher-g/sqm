package io.sqm.render.ansi;

import io.sqm.core.LimitOffset;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.core.LimitOffset.limit;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for LimitOffsetRenderer.
 */
class LimitOffsetRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("No limit or offset renders nothing")
    void no_limit_offset() {
        LimitOffset lo = LimitOffset.of(null, null, false);
        String result = ctx.render(lo).sql();
        assertEquals("", result.trim());
    }

    @Test
    @DisplayName("Only limit renders as LIMIT n")
    void only_limit() {
        LimitOffset lo = limit(10L);
        String result = normalize(ctx.render(lo).sql());
        assertEquals("OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", result);
    }

    @Test
    @DisplayName("Limit with offset renders as LIMIT n OFFSET m")
    void limit_with_offset() {
        LimitOffset lo = LimitOffset.of(10L, 5L);
        String result = normalize(ctx.render(lo).sql());
        assertEquals("OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", result);
    }

    @Test
    @DisplayName("Only offset renders as OFFSET m")
    void only_offset() {
        LimitOffset lo = LimitOffset.of(null, 5L);
        String result = normalize(ctx.render(lo).sql());
        assertEquals("OFFSET 5 ROWS", result);
    }
}
