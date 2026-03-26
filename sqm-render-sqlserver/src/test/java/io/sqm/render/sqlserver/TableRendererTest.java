package io.sqm.render.sqlserver;

import io.sqm.core.Identifier;
import io.sqm.core.Table;
import io.sqm.core.TableHint;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableRendererTest {

    @Test
    void rendersSqlServerLockHints() {
        var table = Table.of(null, Identifier.of("users"), Identifier.of("u"), Table.Inheritance.DEFAULT,
            List.of(TableHint.of("UPDLOCK"), TableHint.of("HOLDLOCK")));

        var sql = RenderContext.of(new SqlServerDialect()).render(table).sql();

        assertTrue(sql.contains("users AS u WITH (UPDLOCK, HOLDLOCK)"));
    }

    @Test
    void rejectsForeignTableHints() {
        var table = Table.of(null, Identifier.of("users"), null, Table.Inheritance.DEFAULT,
            List.of(TableHint.of("USE_INDEX", Identifier.of("idx_users_name"))));

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new SqlServerDialect()).render(table));
    }

    @Test
    void targetTypeIsTable() {
        assertEquals(Table.class, new TableRenderer().targetType());
    }
}