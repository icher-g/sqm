package io.sqm.render.mysql;

import io.sqm.core.Identifier;
import io.sqm.core.Table;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlTableRendererTest {

    @Test
    void rendersTableWithUseIndexHint() {
        var table = Table.of(null, Identifier.of("users"), Identifier.of("u"), Table.Inheritance.DEFAULT,
            List.of(new Table.IndexHint(Table.IndexHintType.USE, Table.IndexHintScope.DEFAULT, List.of(Identifier.of("idx_users_name")))));

        var sql = RenderContext.of(new MySqlDialect()).render(table).sql();

        assertTrue(sql.contains("users AS u USE INDEX (idx_users_name)"));
    }

    @Test
    void rendersAliasBeforeMultipleIndexHintsCanonically() {
        var table = Table.of(null, Identifier.of("users"), Identifier.of("u"), Table.Inheritance.DEFAULT,
            List.of(
                Table.IndexHint.use(Table.IndexHintScope.DEFAULT, List.of(Identifier.of("idx_a"))),
                Table.IndexHint.force(Table.IndexHintScope.JOIN, List.of(Identifier.of("idx_b")))
            ));

        var sql = RenderContext.of(new MySqlDialect()).render(table).sql();

        assertEquals("users AS u USE INDEX (idx_a) FORCE INDEX FOR JOIN (idx_b)", normalize(sql));
    }

    @Test
    void rendersTableWithScopedIndexHint() {
        var table = Table.of(null, Identifier.of("users"), null, Table.Inheritance.DEFAULT,
            List.of(new Table.IndexHint(Table.IndexHintType.FORCE, Table.IndexHintScope.ORDER_BY, List.of(Identifier.of("idx_order")))));

        var sql = RenderContext.of(new MySqlDialect()).render(table).sql();

        assertTrue(sql.contains("FORCE INDEX FOR ORDER BY (idx_order)"));
    }

    @Test
    void rendersGroupByIndexHintWithMultipleIndexes() {
        var table = Table.of(null, Identifier.of("users"), null, Table.Inheritance.DEFAULT,
            List.of(new Table.IndexHint(
                Table.IndexHintType.IGNORE,
                Table.IndexHintScope.GROUP_BY,
                List.of(Identifier.of("idx_a"), Identifier.of("idx_b"))
            )));

        var sql = RenderContext.of(new MySqlDialect()).render(table).sql();

        assertTrue(sql.contains("IGNORE INDEX FOR GROUP BY (idx_a, idx_b)"));
    }

    @Test
    void rejectsIndexHintsInDialectWithoutCapability() {
        var table = Table.of(null, Identifier.of("users"), null, Table.Inheritance.DEFAULT,
            List.of(new Table.IndexHint(Table.IndexHintType.IGNORE, Table.IndexHintScope.DEFAULT, List.of(Identifier.of("idx_users_name")))));

        var renderer = new MySqlTableRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(table, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    @Test
    void targetTypeIsTable() {
        assertEquals(Table.class, new MySqlTableRenderer().targetType());
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
