package io.sqm.render.postgresql;

import io.sqm.core.MergeStatement;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.merge;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MergeStatementRendererTest {

    @Test
    void rendersPostgresMergeFirstSliceWithReturning() {
        var ctx = RenderContext.of(new PostgresDialect(SqlDialectVersion.of(15, 0)));
        MergeStatement statement = merge("users")
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .whenNotMatchedInsert(java.util.List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .result(col("id").toSelectItem())
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals(
            "MERGE INTO users USING src_users AS s ON users.id = s.id WHEN MATCHED THEN UPDATE SET name = s.name WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name) RETURNING id",
            sql
        );
    }

    @Test
    void rejectsMergeBeforePostgres15() {
        var renderer = new MergeStatementRenderer();
        var ctx = RenderContext.of(new PostgresDialect(SqlDialectVersion.of(14, 0)));
        var writer = new DefaultSqlWriter(ctx);
        MergeStatement statement = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(statement, ctx, writer));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
