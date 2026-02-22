package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL {@link LockingClause}.
 *
 * <p>Tests row-level locking syntax:</p>
 * <ul>
 *   <li>{@code FOR UPDATE}</li>
 *   <li>{@code FOR NO KEY UPDATE}</li>
 *   <li>{@code FOR SHARE}</li>
 *   <li>{@code FOR KEY SHARE}</li>
 *   <li>{@code FOR ... OF table1, table2}</li>
 *   <li>{@code FOR ... NOWAIT}</li>
 *   <li>{@code FOR ... SKIP LOCKED}</li>
 * </ul>
 */
@DisplayName("PostgreSQL LockingClauseParser Tests")
class LockingClauseParserTest {

    private final ParseContext parseContext = ParseContext.of(new PostgresSpecs());

    @Test
    @DisplayName("Parse simple FOR UPDATE")
    void parsesSimpleForUpdate() {
        var qResult = parseQuery("SELECT * FROM users FOR UPDATE");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        var lock = query.lockFor();
        assertEquals(LockMode.UPDATE, lock.mode());
        assertTrue(lock.ofTables().isEmpty());
        assertFalse(lock.nowait());
        assertFalse(lock.skipLocked());
    }

    @Test
    @DisplayName("Parse FOR NO KEY UPDATE")
    void parsesForNoKeyUpdate() {
        var qResult = parseQuery("SELECT * FROM users FOR NO KEY UPDATE");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        assertEquals(LockMode.NO_KEY_UPDATE, query.lockFor().mode());
    }

    @Test
    @DisplayName("Parse FOR SHARE")
    void parsesForShare() {
        var qResult = parseQuery("SELECT * FROM users FOR SHARE");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        assertEquals(LockMode.SHARE, query.lockFor().mode());
    }

    @Test
    @DisplayName("Parse FOR KEY SHARE")
    void parsesForKeyShare() {
        var qResult = parseQuery("SELECT * FROM users FOR KEY SHARE");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        assertEquals(LockMode.KEY_SHARE, query.lockFor().mode());
    }

    @Test
    @DisplayName("Parse FOR UPDATE OF single table")
    void parsesForUpdateOfSingleTable() {
        var qResult = parseQuery("SELECT * FROM users u JOIN orders o ON u.id = o.user_id FOR UPDATE OF u");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        var lock = query.lockFor();
        assertEquals(1, lock.ofTables().size());
        assertEquals("u", lock.ofTables().getFirst().identifier());
    }

    @Test
    @DisplayName("Parse FOR UPDATE OF multiple tables")
    void parsesForUpdateOfMultipleTables() {
        var qResult = parseQuery("SELECT * FROM users u JOIN orders o ON u.id = o.user_id FOR UPDATE OF u, o");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        var lock = query.lockFor();
        assertEquals(2, lock.ofTables().size());
        assertEquals("u", lock.ofTables().get(0).identifier());
        assertEquals("o", lock.ofTables().get(1).identifier());
    }

    @Test
    @DisplayName("Parse FOR UPDATE NOWAIT")
    void parsesForUpdateNowait() {
        var qResult = parseQuery("SELECT * FROM users FOR UPDATE NOWAIT");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        var lock = query.lockFor();
        assertEquals(LockMode.UPDATE, lock.mode());
        assertTrue(lock.nowait());
        assertFalse(lock.skipLocked());
    }

    @Test
    @DisplayName("Parse FOR UPDATE SKIP LOCKED")
    void parsesForUpdateSkipLocked() {
        var qResult = parseQuery("SELECT * FROM users FOR UPDATE SKIP LOCKED");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        var lock = query.lockFor();
        assertEquals(LockMode.UPDATE, lock.mode());
        assertFalse(lock.nowait());
        assertTrue(lock.skipLocked());
    }

    @Test
    @DisplayName("Parse FOR UPDATE OF with NOWAIT")
    void parsesForUpdateOfWithNowait() {
        var qResult = parseQuery("SELECT * FROM users u FOR UPDATE OF u NOWAIT");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        var lock = query.lockFor();
        assertEquals(1, lock.ofTables().size());
        assertTrue(lock.nowait());
    }

    @Test
    @DisplayName("Parse FOR SHARE OF with SKIP LOCKED")
    void parsesForShareOfWithSkipLocked() {
        var qResult = parseQuery("SELECT * FROM users u FOR SHARE OF u SKIP LOCKED");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.lockFor());
        
        var lock = query.lockFor();
        assertEquals(LockMode.SHARE, lock.mode());
        assertEquals(1, lock.ofTables().size());
        assertTrue(lock.skipLocked());
    }

    @Test
    @DisplayName("Parse locking clause with WHERE")
    void parsesLockingClauseWithWhere() {
        var qResult = parseQuery("SELECT * FROM users WHERE active = true FOR UPDATE");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.where());
        assertNotNull(query.lockFor());
    }

    @Test
    @DisplayName("Parse locking clause with ORDER BY and LIMIT")
    void parsesLockingClauseWithOrderAndLimit() {
        var qResult = parseQuery("SELECT * FROM users ORDER BY id LIMIT 10 FOR UPDATE");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.orderBy());
        assertNotNull(query.limitOffset().limit());
        assertNotNull(query.lockFor());
    }

    @Test
    @DisplayName("Reject invalid locking mode")
    void rejectsInvalidLockingMode() {
        assertParseError("SELECT * FROM users FOR INVALID");
    }

    @Test
    @DisplayName("Reject incomplete FOR KEY")
    void rejectsIncompleteForKey() {
        assertParseError("SELECT * FROM users FOR KEY");
    }

    @Test
    @DisplayName("Reject incomplete FOR NO KEY")
    void rejectsIncompleteForNoKey() {
        assertParseError("SELECT * FROM users FOR NO KEY");
    }

    @Test
    @DisplayName("Reject FOR UPDATE OF without table list")
    void rejectsForUpdateOfWithoutTargets() {
        assertParseError("SELECT * FROM users FOR UPDATE OF");
    }

    private ParseResult<? extends Query> parseQuery(String sql) {
        return parseContext.parse(Query.class, sql);
    }

    private void assertParseError(String sql) {
        var result = parseQuery(sql);
        if (result.ok()) {
            fail("Expected parse error for: " + sql);
        }
    }
}
