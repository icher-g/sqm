package io.sqm.render.ansi;

import io.sqm.core.LockMode;
import io.sqm.core.LockTarget;
import io.sqm.core.LockingClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LockingClauseRenderer Tests")
class LockingClauseRendererTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new AnsiDialect());
    }

    @Test
    @DisplayName("Render simple FOR UPDATE")
    void renderSimpleForUpdate() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR UPDATE", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR UPDATE in SELECT query")
    void renderForUpdateInSelect() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(update(), List.of(), false, false);
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("FOR UPDATE"));
    }

    @Test
    @DisplayName("Render FOR UPDATE with WHERE clause")
    void renderForUpdateWithWhere() {
        var query = select(col("*"))
            .from(tbl("users"))
            .where(col("active").eq(lit(true)))
            .lockFor(update(), List.of(), false, false);
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("WHERE"));
        assertTrue(normalizeWhitespace(sql).endsWith("FOR UPDATE"));
    }

    @Test
    @DisplayName("FOR SHARE throws unsupported exception")
    void forShareThrows() {
        var clause = LockingClause.of(LockMode.SHARE, List.of(), false, false);
        
        assertThrows(UnsupportedDialectFeatureException.class, 
            () -> renderContext.render(clause));
    }

    @Test
    @DisplayName("FOR NO KEY UPDATE throws unsupported exception")
    void forNoKeyUpdateThrows() {
        var clause = LockingClause.of(LockMode.NO_KEY_UPDATE, List.of(), false, false);
        
        assertThrows(UnsupportedDialectFeatureException.class, 
            () -> renderContext.render(clause));
    }

    @Test
    @DisplayName("FOR KEY SHARE throws unsupported exception")
    void forKeyShareThrows() {
        var clause = LockingClause.of(LockMode.KEY_SHARE, List.of(), false, false);
        
        assertThrows(UnsupportedDialectFeatureException.class, 
            () -> renderContext.render(clause));
    }

    @Test
    @DisplayName("FOR UPDATE OF throws unsupported exception")
    void forUpdateOfThrows() {
        var targets = List.of(LockTarget.of("users"));
        var clause = LockingClause.of(LockMode.UPDATE, targets, false, false);
        
        assertThrows(UnsupportedDialectFeatureException.class, 
            () -> renderContext.render(clause));
    }

    @Test
    @DisplayName("FOR UPDATE NOWAIT throws unsupported exception")
    void forUpdateNowaitThrows() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), true, false);
        
        assertThrows(UnsupportedDialectFeatureException.class, 
            () -> renderContext.render(clause));
    }

    @Test
    @DisplayName("FOR UPDATE SKIP LOCKED throws unsupported exception")
    void forUpdateSkipLockedThrows() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, true);
        
        assertThrows(UnsupportedDialectFeatureException.class, 
            () -> renderContext.render(clause));
    }

    @Test
    @DisplayName("Render FOR UPDATE at end of complex query")
    void renderForUpdateInComplexQuery() {
        var query = select(col("u", "id"), col("u", "name"))
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o"))
                .on(col("u", "id").eq(col("o", "user_id"))))
            .where(col("u", "active").eq(lit(true)))
            .groupBy(group("u", "id"), group("u", "name"))
            .having(func("count", starArg()).gt(lit(5)))
            .orderBy(order("u", "name"))
            .limit(10)
            .lockFor(update(), List.of(), false, false);
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).endsWith("FOR UPDATE"));
    }

    @Test
    @DisplayName("Query without locking clause has no FOR UPDATE")
    void queryWithoutLockingClause() {
        var query = select(col("*"))
            .from(tbl("users"));
        
        var sql = renderContext.render(query).sql();
        
        assertFalse(normalizeWhitespace(sql).contains("FOR UPDATE"));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
