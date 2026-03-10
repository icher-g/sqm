package io.sqm.render.mysql;

import io.sqm.core.InsertStatement;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MySqlInsertStatementRendererTest {

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
            tbl("users"),
            java.util.List.of(),
            row(lit(1)),
            java.util.List.of(),
            InsertStatement.OnConflictAction.DO_UPDATE,
            java.util.List.of(set("name", lit("alice2"))),
            col("id").eq(lit(1)),
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

        var renderer = new MySqlInsertStatementRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    @Test
    void rejectsReplaceIntoInDialectWithoutCapability() {
        InsertStatement statement = insert("users")
            .replace()
            .values(row(lit(1)))
            .build();

        var renderer = new MySqlInsertStatementRenderer();
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

        var renderer = new MySqlInsertStatementRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
