package io.sqm.render.oracle;

import io.sqm.core.LockWaitMode;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleTimeZoneAndLockingRendererTest {
    private final RenderContext ctx = RenderContext.of(new OracleDialect());

    @Test
    void rendersAtTimeZoneExpression() {
        var query = select(col("created_at").atTimeZone(lit("UTC")))
            .from(tbl("users"))
            .build();

        assertEquals("SELECT created_at AT TIME ZONE 'UTC' FROM users", normalize(ctx.render(query).sql()));
    }

    @Test
    void rendersForUpdateOfWait() {
        var query = select(star())
            .from(tbl("users").as("u"))
            .lockFor(update(), ofTables("u"), LockWaitMode.WAIT, lit(5))
            .build();

        assertEquals("SELECT * FROM users u FOR UPDATE OF u WAIT 5", normalize(ctx.render(query).sql()));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
