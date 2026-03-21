package io.sqm.render.sqlserver;

import io.sqm.core.ResultInto;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultIntoRendererTest {

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersIntoTargetWithoutColumns() {
        var ctx = RenderContext.of(new SqlServerDialect());
        ResultInto into = ResultInto.of(tbl("dest"));

        var sql = normalize(ctx.render(into).sql());

        assertEquals("INTO dest", sql);
    }

    @Test
    void rendersIntoTargetWithColumns() {
        var ctx = RenderContext.of(new SqlServerDialect());
        ResultInto into = ResultInto.of(tbl("dest"), java.util.List.of(id("col_a"), id("col_b")));

        var sql = normalize(ctx.render(into).sql());

        assertEquals("INTO dest (col_a, col_b)", sql);
    }

    @Test
    void rendersIntoTableVariableTarget() {
        var ctx = RenderContext.of(new SqlServerDialect());
        ResultInto into = ResultInto.of(io.sqm.dsl.Dsl.tableVar("dest"), java.util.List.of(id("col_a"), id("col_b")));

        var sql = normalize(ctx.render(into).sql());

        assertEquals("INTO @dest (col_a, col_b)", sql);
    }

    @Test
    void rejectsNonTableIntoTarget() {
        var ctx = RenderContext.of(new SqlServerDialect());
        ResultInto into = ResultInto.of(tbl(select(io.sqm.dsl.Dsl.lit(1L)).build()).as("d"));

        var error = assertThrows(UnsupportedOperationException.class, () -> ctx.render(into));

        assertEquals("SQL Server OUTPUT INTO currently supports base tables and table variables only", error.getMessage());
    }
}
