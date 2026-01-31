package io.sqm.render.ansi;

import io.sqm.core.LimitOffset;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.core.Expression;
import io.sqm.render.spi.PaginationStyle;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.core.LimitOffset.limit;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    @DisplayName("LIMIT ALL without offset renders nothing for OFFSET/FETCH style")
    void limit_all_only_offset_fetch() {
        LimitOffset lo = LimitOffset.all();
        String result = normalize(ctx.render(lo).sql());
        assertEquals("", result);
    }

    @Test
    @DisplayName("LIMIT ALL with offset renders only OFFSET for OFFSET/FETCH style")
    void limit_all_with_offset_offset_fetch() {
        LimitOffset lo = LimitOffset.of(null, Expression.literal(7L), true);
        String result = normalize(ctx.render(lo).sql());
        assertEquals("OFFSET 7 ROWS", result);
    }

    @Test
    @DisplayName("LIMIT ALL renders for LIMIT/OFFSET style")
    void limit_all_limit_offset_style() {
        var ctx = RenderContext.of(new LimitOffsetStyleDialect());
        LimitOffset lo = LimitOffset.all();
        String result = normalize(ctx.render(lo).sql());
        assertEquals("LIMIT ALL", result);
    }

    @Test
    @DisplayName("LIMIT ALL with offset renders for LIMIT/OFFSET style")
    void limit_all_with_offset_limit_offset_style() {
        var ctx = RenderContext.of(new LimitOffsetStyleDialect());
        LimitOffset lo = LimitOffset.of(null, Expression.literal(3L), true);
        String result = normalize(ctx.render(lo).sql());
        assertEquals("LIMIT ALL OFFSET 3", result);
    }

    @Test
    @DisplayName("TOP-only dialect rejects OFFSET")
    void top_only_dialect_rejects_offset() {
        var ctx = RenderContext.of(new TopOnlyDialect());
        LimitOffset lo = LimitOffset.of(null, 1L);
        assertThrows(UnsupportedOperationException.class, () -> ctx.render(lo));
    }

    private static final class LimitOffsetStyleDialect extends AnsiDialect {
        private final PaginationStyle paginationStyle = new LimitOffsetPaginationStyle();

        @Override
        public String name() {
            return "limit-offset";
        }

        @Override
        public PaginationStyle paginationStyle() {
            return paginationStyle;
        }
    }

    private static final class TopOnlyDialect extends AnsiDialect {
        private final PaginationStyle paginationStyle = new TopOnlyPaginationStyle();

        @Override
        public String name() {
            return "top-only";
        }

        @Override
        public PaginationStyle paginationStyle() {
            return paginationStyle;
        }
    }

    private static final class LimitOffsetPaginationStyle implements PaginationStyle {
        @Override
        public boolean supportsLimitOffset() {
            return true;
        }

        @Override
        public boolean supportsOffsetFetch() {
            return false;
        }

        @Override
        public boolean supportsTop() {
            return false;
        }
    }

    private static final class TopOnlyPaginationStyle implements PaginationStyle {
        @Override
        public boolean supportsLimitOffset() {
            return false;
        }

        @Override
        public boolean supportsOffsetFetch() {
            return false;
        }

        @Override
        public boolean supportsTop() {
            return true;
        }
    }
}
