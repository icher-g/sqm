package io.sqm.render.mysql;

import io.sqm.core.InsertStatement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsertStatementRendererTest {

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersInsertIgnoreStatement() {
        var statement = insert("users")
            .ignore()
            .columns(id("id"))
            .values(row(lit(1)))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals("INSERT IGNORE INTO users (id) VALUES (1)", normalize(sql));
    }

    @Test
    void rendersReplaceIntoStatement() {
        var statement = insert("users")
            .replace()
            .columns(id("id"), id("name"))
            .values(row(lit(1), lit("alice")))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals("REPLACE INTO users (id, name) VALUES (1, 'alice')", normalize(sql));
    }

    @Test
    void rendersOnDuplicateKeyUpdateStatement() {
        var statement = insert("users")
            .columns(id("id"), id("name"))
            .values(row(lit(1), lit("alice")))
            .onConflictDoUpdate(java.util.List.of(set("name", lit("alice2"))))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals("INSERT INTO users (id, name) VALUES (1, 'alice') ON DUPLICATE KEY UPDATE name = 'alice2'", normalize(sql));
    }

    @Test
    void rendersOnDuplicateKeyUpdateWithQualifiedTarget() {
        var statement = insert("users")
            .columns(id("id"), id("name"))
            .values(row(lit(1), lit("alice")))
            .onConflictDoUpdate(java.util.List.of(set("users", "name", lit("alice2"))))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals("INSERT INTO users (id, name) VALUES (1, 'alice') ON DUPLICATE KEY UPDATE users.name = 'alice2'", normalize(sql));
    }

    @Test
    void rejectsPostgresOnConflictShapeInMysqlRenderer() {
        var statement = insert("users")
            .values(row(lit(1)))
            .onConflictDoNothing(id("id"))
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect()).render(statement));
    }

    @Test
    void rejectsOnDuplicateKeyUpdateWithConflictTarget() {
        var statement = insert("users")
            .values(row(lit(1)))
            .onConflictDoUpdate(java.util.List.of(id("id")), java.util.List.of(set("name", lit("alice2"))))
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect()).render(statement));
    }

    @Test
    void rejectsOnDuplicateKeyUpdateWithWhereClause() {
        InsertStatement statement = InsertStatement.of(
            InsertStatement.InsertMode.STANDARD,
            tbl("users"),
            java.util.List.of(),
            row(lit(1)),
            java.util.List.of(),
            InsertStatement.OnConflictAction.DO_UPDATE,
            java.util.List.of(set("name", lit("alice2"))),
            col("id").eq(lit(1)),
            null,
            java.util.List.of());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect()).render(statement));
    }

    @Test
    void rejectsInsertIgnoreInDialectWithoutCapability() {
        InsertStatement statement = insert("users")
            .ignore()
            .values(row(lit(1)))
            .build();

        var renderer = new InsertStatementRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    @Test
    void rendersInsertStatementHints() {
        InsertStatement statement = insert("users")
            .hint("MAX_EXECUTION_TIME", 1000)
            .values(row(lit(1)))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals("INSERT /*+ MAX_EXECUTION_TIME(1000) */ INTO users VALUES (1)", normalize(sql));
    }

    @Test
    void rejectsReplaceIntoInDialectWithoutCapability() {
        InsertStatement statement = insert("users")
            .replace()
            .values(row(lit(1)))
            .build();

        var renderer = new InsertStatementRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    @Test
    void rejectsOnDuplicateKeyUpdateInDialectWithoutCapability() {
        InsertStatement statement = insert("users")
            .values(row(lit(1)))
            .onConflictDoUpdate(java.util.List.of(set("name", lit("alice2"))))
            .build();

        var renderer = new InsertStatementRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    @Test
    void rejectsInsertReturningInMysql80Renderer() {
        InsertStatement statement = insert("users")
            .values(row(lit(1)))
            .result(col("id"))
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect()).render(statement));
    }

    @Test
    void rejectsInsertReturningInMysql57Renderer() {
        InsertStatement statement = insert("users")
            .values(row(lit(1)))
            .result(col("id"))
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect(SqlDialectVersion.of(5, 7))).render(statement));
    }

    @Test
    void rendersInsertReturningWhenCapabilityIsEnabled() {
        InsertStatement statement = insert("users")
            .values(row(lit(1)))
            .result(col("id"))
            .build();

        var sql = RenderContext.of(new ReturningMySqlDialect()).render(statement).sql();

        assertEquals("INSERT INTO users VALUES (1) RETURNING id", normalize(sql));
    }

    private static final class ReturningMySqlDialect extends MySqlDialect {
        @Override
        public DialectCapabilities capabilities() {
            var delegate = super.capabilities();
            return feature -> feature == SqlFeature.DML_RESULT_CLAUSE || delegate.supports(feature);
        }
    }
}
