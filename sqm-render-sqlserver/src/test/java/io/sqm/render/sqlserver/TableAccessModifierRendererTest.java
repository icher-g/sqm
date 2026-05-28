package io.sqm.render.sqlserver;

import io.sqm.core.SampledTable;
import io.sqm.core.TableSampleSpec;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TableAccessModifierRendererTest {
    private final RenderContext ctx = RenderContext.of(new SqlServerDialect());

    @Test
    void rendersSystemTimeAsOf() {
        var table = tbl("orders").withVersion(asOfTimestamp(lit(42)));

        assertEquals("orders FOR SYSTEM_TIME AS OF 42", normalize(ctx.render(table).sql()));
    }

    @Test
    void rendersSystemTimeRangeSelectors() {
        assertEquals("orders FOR SYSTEM_TIME FROM 10 TO 20",
            normalize(ctx.render(tbl("orders").withVersion(tableVersionFromTo(lit(10), lit(20)))).sql()));
        assertEquals("orders FOR SYSTEM_TIME BETWEEN 10 AND 20",
            normalize(ctx.render(tbl("orders").withVersion(tableVersionBetween(lit(10), lit(20)))).sql()));
        assertEquals("orders FOR SYSTEM_TIME CONTAINED IN (10, 20)",
            normalize(ctx.render(tbl("orders").withVersion(tableVersionContainedIn(lit(10), lit(20)))).sql()));
        assertEquals("orders FOR SYSTEM_TIME ALL",
            normalize(ctx.render(tbl("orders").withVersion(tableVersionAll())).sql()));
    }

    @Test
    void rendersTableSampleRows() {
        var table = SampledTable.of(
            tbl("users"),
            TableSampleSpec.of(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.ROWS, lit(100), lit(42)));

        assertEquals("users TABLESAMPLE (100 ROWS) REPEATABLE (42)", normalize(ctx.render(table).sql()));
    }

    @Test
    void rendersTableSamplePercentAndAliasWithoutSeed() {
        var table = SampledTable.of(
            tbl("users"),
            TableSampleSpec.of(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.PERCENT, lit(10), null),
            id("u"));

        assertEquals("users TABLESAMPLE (10 PERCENT) AS u", normalize(ctx.render(table).sql()));
    }

    @Test
    void rendersTableSampleWithoutUnit() {
        var table = SampledTable.of(
            tbl("users"),
            TableSampleSpec.of(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.UNSPECIFIED, lit(100), null));

        assertEquals("users TABLESAMPLE (100)", normalize(ctx.render(table).sql()));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
