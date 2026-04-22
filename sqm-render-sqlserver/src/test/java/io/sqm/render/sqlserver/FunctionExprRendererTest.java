package io.sqm.render.sqlserver;

import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FunctionExprRendererTest {

    @Test
    void rendersFirstWaveSqlServerFunctions() {
        var ctx = RenderContext.of(new SqlServerDialect());

        assertEquals("LEN(name)", ctx.render(len(col("name"))).sql());
        assertEquals("DATALENGTH(payload)", ctx.render(dataLength(col("payload"))).sql());
        assertEquals("GETDATE()", ctx.render(getDate()).sql());
        assertEquals("ISNULL(name, 'unknown')", ctx.render(isNullFn(col("name"), lit("unknown"))).sql());
        assertEquals("STRING_AGG(name, ',') WITHIN GROUP (ORDER BY name)",
            ctx.render(stringAgg(col("name"), lit(",")).withinGroup(orderBy("name"))).sql());
    }

    @Test
    void rendersDateAddAndDateDiffDatepartsWithoutQuotes() {
        var ctx = RenderContext.of(new SqlServerDialect());

        assertEquals("DATEADD(day, 1, created_at)", ctx.render(dateAdd("day", lit(1), col("created_at"))).sql());
        assertEquals("DATEDIFF(day, start_at, end_at)", ctx.render(dateDiff("day", col("start_at"), col("end_at"))).sql());
    }
}
