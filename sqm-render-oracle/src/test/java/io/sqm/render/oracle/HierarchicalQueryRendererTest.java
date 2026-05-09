package io.sqm.render.oracle;

import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class HierarchicalQueryRendererTest {
    @Test
    void rendersOracleHierarchicalQuery() {
        var query = select(col("id"), col("parent_id"), col("LEVEL"))
            .from(tbl("categories"))
            .hierarchical(hierarchy(
                col("parent_id").isNull(),
                prior(col("id")).eq(col("parent_id")),
                true,
                orderBy(col("name"))
            ))
            .build();

        var sql = RenderContext.of(new OracleDialect()).render(query).sql();

        assertTrue(sql.contains("START WITH parent_id IS NULL"));
        assertTrue(sql.contains("CONNECT BY NOCYCLE PRIOR id = parent_id"));
        assertTrue(sql.contains("ORDER SIBLINGS BY name"));
    }

    @Test
    void rejectsHierarchicalQueryForAnsiDialect() {
        var query = select(col("id"))
            .from(tbl("categories"))
            .hierarchical(hierarchy(null, prior(col("id")).eq(col("parent_id")), false))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> RenderContext.of(new AnsiDialect()).render(query));
    }
}
