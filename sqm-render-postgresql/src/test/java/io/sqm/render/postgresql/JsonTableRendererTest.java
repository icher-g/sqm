package io.sqm.render.postgresql;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonTableRendererTest {
    @Test
    void rendersJsonTableInPostgres17AndLater() {
        var table = jsonTable(
            col("payload"),
            jsonPath("$.items[*]"),
            jsonScalar("id", type("NUMBER"), jsonPath("$.id")), jsonOrdinality("ord")
        ).as("jt");

        var sql = RenderContext.of(new PostgresDialect(SqlDialectVersion.of(17, 0))).render(table).sql();

        assertEquals(
            "JSON_TABLE ( payload, '$.items[*]' COLUMNS ( id NUMBER PATH '$.id', ord FOR ORDINALITY ) ) AS jt",
            sql.replaceAll("\\s+", " ").trim()
        );
    }

    @Test
    void rejectsJsonTableBeforePostgres17() {
        var table = jsonTable(col("payload"), jsonPath("$"), jsonScalar("id", type("NUMBER"), jsonPath("$.id")));

        assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new PostgresDialect(SqlDialectVersion.of(16, 0))).render(table).sql()
        );
    }
}
