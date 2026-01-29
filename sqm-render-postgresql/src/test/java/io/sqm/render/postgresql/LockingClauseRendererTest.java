package io.sqm.render.postgresql;

import io.sqm.core.*;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostgreSQL {@link LockingClauseRenderer}.
 */
@DisplayName("PostgreSQL LockingClauseRenderer Tests")
class LockingClauseRendererTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render simple FOR UPDATE")
    void rendersSimpleForUpdate() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR UPDATE", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR NO KEY UPDATE")
    void rendersForNoKeyUpdate() {
        var clause = LockingClause.of(LockMode.NO_KEY_UPDATE, List.of(), false, false);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR NO KEY UPDATE", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR SHARE")
    void rendersForShare() {
        var clause = LockingClause.of(LockMode.SHARE, List.of(), false, false);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR SHARE", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR KEY SHARE")
    void rendersForKeyShare() {
        var clause = LockingClause.of(LockMode.KEY_SHARE, List.of(), false, false);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR KEY SHARE", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR UPDATE OF single table")
    void rendersForUpdateOfSingleTable() {
        var targets = List.of(LockTarget.of("users"));
        var clause = LockingClause.of(LockMode.UPDATE, targets, false, false);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR UPDATE OF users", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR UPDATE OF multiple tables")
    void rendersForUpdateOfMultipleTables() {
        var targets = List.of(LockTarget.of("users"), LockTarget.of("orders"));
        var clause = LockingClause.of(LockMode.UPDATE, targets, false, false);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR UPDATE OF users, orders", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR UPDATE NOWAIT")
    void rendersForUpdateNowait() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), true, false);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR UPDATE NOWAIT", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR UPDATE SKIP LOCKED")
    void rendersForUpdateSkipLocked() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, true);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR UPDATE SKIP LOCKED", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR UPDATE OF with NOWAIT")
    void rendersForUpdateOfWithNowait() {
        var targets = List.of(LockTarget.of("users"));
        var clause = LockingClause.of(LockMode.UPDATE, targets, true, false);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR UPDATE OF users NOWAIT", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR SHARE OF with SKIP LOCKED")
    void rendersForShareOfWithSkipLocked() {
        var targets = List.of(LockTarget.of("users"));
        var clause = LockingClause.of(LockMode.SHARE, targets, false, true);
        var sql = renderContext.render(clause).sql();
        
        assertEquals("FOR SHARE OF users SKIP LOCKED", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render FOR UPDATE in SELECT query")
    void rendersForUpdateInSelectQuery() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(update(), List.of(), false, false);
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).endsWith("FOR UPDATE"));
    }

    @Test
    @DisplayName("Render locking clause with WHERE")
    void rendersLockingClauseWithWhere() {
        var query = select(col("*"))
            .from(tbl("users"))
            .where(col("active").eq(lit(true)))
            .lockFor(update(), List.of(), false, false);
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("WHERE"));
        assertTrue(normalizeWhitespace(sql).endsWith("FOR UPDATE"));
    }

    @Test
    @DisplayName("Render locking clause with ORDER BY and LIMIT")
    void rendersLockingClauseWithOrderAndLimit() {
        var query = select(col("*"))
            .from(tbl("users"))
            .orderBy(order("id"))
            .limit(10)
            .lockFor(update(), List.of(), false, false);
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("ORDER BY"));
        assertTrue(normalizeWhitespace(sql).contains("LIMIT"));
        assertTrue(normalizeWhitespace(sql).endsWith("FOR UPDATE"));
    }

    @Test
    @DisplayName("Render FOR UPDATE OF with table aliases")
    void rendersForUpdateOfWithAliases() {
        var targets = ofTables("u", "o");
        var query = select(col("*"))
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
            .lockFor(update(), targets, false, false);
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("FOR UPDATE OF u, o"));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
