package io.sqm.render.sqlserver;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtTimeZoneExprRendererTest {

    @Test
    void rendersAtTimeZoneForSupportedSqlServerVersions() {
        var query = select(col("created_at").atTimeZone(lit("UTC"))).from(tbl("users")).build();

        var rendered = RenderContext.of(new SqlServerDialect(SqlDialectVersion.of(2019, 0))).render(query);

        assertEquals("SELECT created_at AT TIME ZONE 'UTC' FROM users", normalize(rendered.sql()));
    }

    @Test
    void rejectsAtTimeZoneForUnsupportedSqlServerVersions() {
        var query = select(col("created_at").atTimeZone(lit("UTC"))).from(tbl("users")).build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> RenderContext.of(new SqlServerDialect(SqlDialectVersion.of(2014, 0))).render(query)
        );
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
