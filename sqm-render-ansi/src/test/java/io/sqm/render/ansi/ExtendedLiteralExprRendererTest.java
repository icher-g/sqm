package io.sqm.render.ansi;

import io.sqm.core.*;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExtendedLiteralExprRendererTest extends BaseValuesRendererTest {

    @Test
    void renders_date_time_timestamp_literals() {
        var ctx = RenderContext.of(new AnsiDialect());

        assertSqlAndParams(ctx.render(DateLiteralExpr.of("2020-01-01")), "DATE '2020-01-01'", List.of());
        assertSqlAndParams(ctx.render(TimeLiteralExpr.of("10:11:12", TimeZoneSpec.WITH_TIME_ZONE)),
            "TIME WITH TIME ZONE '10:11:12'", List.of());
        assertSqlAndParams(ctx.render(TimestampLiteralExpr.of("2020-01-01 00:00:00", TimeZoneSpec.WITHOUT_TIME_ZONE)),
            "TIMESTAMP WITHOUT TIME ZONE '2020-01-01 00:00:00'", List.of());
    }

    @Test
    void renders_bit_and_hex_literals() {
        var ctx = RenderContext.of(new AnsiDialect());

        assertSqlAndParams(ctx.render(BitStringLiteralExpr.of("1010")), "B'1010'", List.of());
        assertSqlAndParams(ctx.render(HexStringLiteralExpr.of("FF")), "X'FF'", List.of());
    }

    @Test
    void renders_interval_literal() {
        var ctx = RenderContext.of(new AnsiDialect());

        assertSqlAndParams(ctx.render(IntervalLiteralExpr.of("1")), "INTERVAL '1'", List.of());
    }

    @Test
    void rejects_postgres_only_literals() {
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(EscapeStringLiteralExpr.of("it\\'s")));
        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(DollarStringLiteralExpr.of("tag", "value")));
    }
}
