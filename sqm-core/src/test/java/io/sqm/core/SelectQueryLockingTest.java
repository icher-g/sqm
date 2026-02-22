package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SelectQuery Locking Tests")
class SelectQueryLockingTest {

    @Test
    @DisplayName("Add FOR UPDATE to query")
    void addForUpdateToQuery() {
        var lockClause = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var query = select(col("id"), col("name"))
            .from(tbl("users"))
            .lockFor(lockClause)
            .build();
        
        assertNotNull(query.lockFor());
        assertEquals(LockMode.UPDATE, query.lockFor().mode());
    }

    @Test
    @DisplayName("Add FOR UPDATE using convenience method")
    void addForUpdateConvenience() {
        var query = select(col("id"))
            .from(tbl("users"))
            .lockFor(update(), List.of(), false, false)
            .build();
        
        assertNotNull(query.lockFor());
        assertEquals(LockMode.UPDATE, query.lockFor().mode());
    }

    @Test
    @DisplayName("Add FOR UPDATE OF with table targets")
    void addForUpdateOf() {
        var targets = ofTables("users", "orders");
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(update(), targets, false, false)
            .build();
        
        assertNotNull(query.lockFor());
        assertEquals(2, query.lockFor().ofTables().size());
    }

    @Test
    @DisplayName("Add FOR UPDATE NOWAIT")
    void addForUpdateNowait() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(update(), List.of(), true, false)
            .build();
        
        assertTrue(query.lockFor().nowait());
        assertFalse(query.lockFor().skipLocked());
    }

    @Test
    @DisplayName("Add FOR UPDATE SKIP LOCKED")
    void addForUpdateSkipLocked() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(update(), List.of(), false, true)
            .build();
        
        assertFalse(query.lockFor().nowait());
        assertTrue(query.lockFor().skipLocked());
    }

    @Test
    @DisplayName("Query without locking clause returns null")
    void queryWithoutLockingClause() {
        var query = select(col("*"))
            .from(tbl("users"))
            .build();
        
        assertNull(query.lockFor());
    }

    @Test
    @DisplayName("Complex query with FOR UPDATE")
    void complexQueryWithLocking() {
        var query = select(col("u", "id"), col("o", "order_id"))
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o"))
                .on(col("u", "id").eq(col("o", "user_id"))))
            .where(col("u", "active").eq(lit(true)))
            .lockFor(update(), ofTables("u", "o"), false, false)
            .build();
        
        assertNotNull(query.lockFor());
        assertEquals(2, query.lockFor().ofTables().size());
    }

    @Test
    @DisplayName("FOR SHARE mode")
    void forShareMode() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(share(), List.of(), false, false)
            .build();
        
        assertEquals(LockMode.SHARE, query.lockFor().mode());
    }

    @Test
    @DisplayName("FOR NO KEY UPDATE mode")
    void forNoKeyUpdateMode() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(noKeyUpdate(), List.of(), false, false)
            .build();
        
        assertEquals(LockMode.NO_KEY_UPDATE, query.lockFor().mode());
    }

    @Test
    @DisplayName("FOR KEY SHARE mode")
    void forKeyShareMode() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(keyShare(), List.of(), false, false)
            .build();
        
        assertEquals(LockMode.KEY_SHARE, query.lockFor().mode());
    }
}
