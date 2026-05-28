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
    void rendersTableSampleRows() {
        var table = SampledTable.of(
            tbl("users"),
            TableSampleSpec.of(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.ROWS, lit(100), lit(42)));

        assertEquals("users TABLESAMPLE (100 ROWS) REPEATABLE (42)", normalize(ctx.render(table).sql()));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
