package io.sqm.render.oracle;

import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleRenderSmokeTest {

    @Test
    void rendersSimpleQueryWithOracleDialect() {
        var query = select(lit(1L)).from(tbl("dual")).build();
        var sql = RenderContext.of(new OracleDialect()).render(query).sql();

        assertEquals("SELECT 1 FROM dual", sql.replaceAll("\\s+", " ").trim());
    }
}
