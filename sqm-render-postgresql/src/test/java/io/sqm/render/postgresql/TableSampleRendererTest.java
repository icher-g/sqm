package io.sqm.render.postgresql;

import io.sqm.core.SampledTable;
import io.sqm.core.TableSampleSpec;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableSampleRendererTest {
    private final RenderContext ctx = RenderContext.of(new PostgresDialect());

    @Test
    void rendersTableSample() {
        var table = SampledTable.of(
            tbl("users"),
            TableSampleSpec.of(TableSampleSpec.SampleMethod.BERNOULLI, TableSampleSpec.SampleUnit.PERCENT, lit(10), lit(42)));

        assertEquals("users TABLESAMPLE BERNOULLI (10) REPEATABLE (42)", normalize(ctx.render(table).sql()));
    }

    @Test
    void rendersSystemTableSampleWithAliasAndNoSeed() {
        var table = SampledTable.of(
            tbl("users"),
            TableSampleSpec.of(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.UNSPECIFIED, lit(25), null),
            id("u"));

        assertEquals("users TABLESAMPLE SYSTEM (25) AS u", normalize(ctx.render(table).sql()));
    }

    @Test
    void rejectsOracleBlockSampleMethod() {
        var table = SampledTable.of(
            tbl("users"),
            TableSampleSpec.of(TableSampleSpec.SampleMethod.BLOCK, TableSampleSpec.SampleUnit.PERCENT, lit(10), null));

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class, () -> ctx.render(table));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
