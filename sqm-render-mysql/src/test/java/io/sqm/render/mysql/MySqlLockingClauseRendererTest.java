package io.sqm.render.mysql;

import io.sqm.core.LockMode;
import io.sqm.core.LockingClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlLockingClauseRendererTest {

    private final RenderContext ctx = RenderContext.of(new MySqlDialect());

    @Test
    void rendersForShare() {
        var sql = ctx.render(LockingClause.of(LockMode.SHARE, List.of(), false, false)).sql();

        assertEquals("FOR SHARE", normalize(sql));
    }

    @Test
    void rendersForUpdateNowait() {
        var sql = ctx.render(LockingClause.of(LockMode.UPDATE, List.of(), true, false)).sql();

        assertEquals("FOR UPDATE NOWAIT", normalize(sql));
    }

    @Test
    void rendersForUpdateSkipLocked() {
        var sql = ctx.render(LockingClause.of(LockMode.UPDATE, List.of(), false, true)).sql();

        assertEquals("FOR UPDATE SKIP LOCKED", normalize(sql));
    }

    @Test
    void rejectsForNoKeyUpdate_withDeterministicMessage() {
        var ex = assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> ctx.render(LockingClause.of(LockMode.NO_KEY_UPDATE, List.of(), false, false))
        );

        assertTrue(ex.getMessage().contains("Feature not supported by dialect [MySQL]: FOR NO KEY UPDATE"));
    }

    @Test
    void rejectsForKeyShare_withDeterministicMessage() {
        var ex = assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> ctx.render(LockingClause.of(LockMode.KEY_SHARE, List.of(), false, false))
        );

        assertTrue(ex.getMessage().contains("Feature not supported by dialect [MySQL]: FOR KEY SHARE"));
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
