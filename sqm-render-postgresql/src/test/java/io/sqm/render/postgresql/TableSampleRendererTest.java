package io.sqm.render.postgresql;

import io.sqm.core.SampledTable;
import io.sqm.core.TableSampleSpec;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TableSampleRendererTest {
    private final RenderContext ctx = RenderContext.of(new PostgresDialect());

    @Test
    void rendersTableSample() {
        var table = SampledTable.of(
            tbl("users"),
            TableSampleSpec.of(TableSampleSpec.SampleMethod.BERNOULLI, TableSampleSpec.SampleUnit.PERCENT, lit(10), lit(42)));

        assertEquals("users TABLESAMPLE BERNOULLI (10) REPEATABLE (42)", normalize(ctx.render(table).sql()));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
