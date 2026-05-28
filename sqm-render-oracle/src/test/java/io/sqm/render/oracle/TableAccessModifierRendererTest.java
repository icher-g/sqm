package io.sqm.render.oracle;

import io.sqm.core.SampledTable;
import io.sqm.core.Table;
import io.sqm.core.TableSampleSpec;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TableAccessModifierRendererTest {
    private final RenderContext ctx = RenderContext.of(new OracleDialect());

    @Test
    void rendersFlashbackAndPartition() {
        var table = tbl("sales")
            .withVersion(asOfScn(param("scn")))
            .withPartitionSpec(tablePartition("sales_q1"));

        assertEquals("sales AS OF SCN :scn PARTITION (sales_q1)", normalize(ctx.render(table).sql()));
    }

    @Test
    void rendersSampleBlockSeed() {
        var sampled = SampledTable.of(
            Table.of(io.sqm.core.Identifier.of("users")),
            TableSampleSpec.of(TableSampleSpec.SampleMethod.BLOCK, TableSampleSpec.SampleUnit.PERCENT, lit(10), lit(42)),
            io.sqm.core.Identifier.of("u"));

        assertEquals("users SAMPLE BLOCK (10) SEED (42) AS u", normalize(ctx.render(sampled).sql()));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
