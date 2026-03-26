package io.sqm.render.mysql;

import io.sqm.core.QualifiedName;
import io.sqm.core.TableHint;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.dsl.Dsl;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlHintRenderSupportTest {

    @Test
    void renderStatementHintsLeavesWriterUntouchedWhenEmpty() {
        var ctx = RenderContext.of(new MySqlDialect());
        var writer = new DefaultSqlWriter(ctx);

        MySqlHintRenderSupport.renderStatementHints(java.util.List.of(), "statement hints", ctx, writer);

        assertEquals("", writer.toText(java.util.List.of()).sql());
    }

    @Test
    void renderStatementHintsRejectsDialectsWithoutCommentHintSupport() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> MySqlHintRenderSupport.renderStatementHints(java.util.List.of(Dsl.statementHint("MAX_EXECUTION_TIME", 1000)), "statement hints", ctx, writer)
        );
    }

    @Test
    void rendersMixedHintArgKinds() {
        var ctx = RenderContext.of(new MySqlDialect());
        var writer = new DefaultSqlWriter(ctx);

        MySqlHintRenderSupport.renderHintArgs(
            TableHint.of("GENERIC_HINT", "users", QualifiedName.of("app", "users"), 1000),
            ctx,
            writer
        );

        assertEquals("users, app.users, 1000", writer.toText(java.util.List.of()).sql());
    }

    @Test
    void classifiesAndMapsMysqlTableHintFamilies() {
        var useIndex = TableHint.of("USE_INDEX", "idx_users_name");
        var forceOrderBy = TableHint.of("FORCE_INDEX_FOR_ORDER_BY", "idx_users_name");

        assertTrue(MySqlHintRenderSupport.isIndexHint(useIndex));
        assertFalse(MySqlHintRenderSupport.isIndexHint(TableHint.of("NOLOCK")));
        assertTrue(MySqlHintRenderSupport.isSqlServerLockHint(TableHint.of("HOLDLOCK")));
        assertFalse(MySqlHintRenderSupport.isSqlServerLockHint(useIndex));
        assertEquals("USE", MySqlHintRenderSupport.indexHintKeyword(useIndex));
        assertEquals("FORCE", MySqlHintRenderSupport.indexHintKeyword(forceOrderBy));
        assertNull(MySqlHintRenderSupport.indexHintScope(useIndex));
        assertEquals("ORDER BY", MySqlHintRenderSupport.indexHintScope(forceOrderBy));
    }

    @Test
    void rejectsUnsupportedIndexHintKeywordFamilies() {
        assertThrows(IllegalArgumentException.class, () -> MySqlHintRenderSupport.indexHintKeyword(TableHint.of("INDEX", "idx_users_name")));
    }

    @Test
    void renderHintProducesCanonicalMysqlHintText() {
        var ctx = RenderContext.of(new MySqlDialect());

        var sql = MySqlHintRenderSupport.renderHint(
            Dsl.statementHint("QUALIFIED_HINT", QualifiedName.of("app", "users"), Dsl.lit("x")),
            ctx
        );

        assertEquals("QUALIFIED_HINT(app.users, 'x')", sql);
    }
}
