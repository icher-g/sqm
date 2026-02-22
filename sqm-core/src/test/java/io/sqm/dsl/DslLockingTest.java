package io.sqm.dsl;

import io.sqm.core.LockMode;
import io.sqm.core.LockTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DSL Locking Tests")
class DslLockingTest {

    @Test
    @DisplayName("update() returns UPDATE mode")
    void updateMode() {
        assertEquals(LockMode.UPDATE, update());
    }

    @Test
    @DisplayName("share() returns SHARE mode")
    void shareMode() {
        assertEquals(LockMode.SHARE, share());
    }

    @Test
    @DisplayName("noKeyUpdate() returns NO_KEY_UPDATE mode")
    void noKeyUpdateMode() {
        assertEquals(LockMode.NO_KEY_UPDATE, noKeyUpdate());
    }

    @Test
    @DisplayName("keyShare() returns KEY_SHARE mode")
    void keyShareMode() {
        assertEquals(LockMode.KEY_SHARE, keyShare());
    }

    @Test
    @DisplayName("ofTables() creates list of lock targets")
    void ofTablesCreatesTargets() {
        var targets = ofTables("users", "orders");
        
        assertNotNull(targets);
        assertEquals(2, targets.size());
        assertEquals("users", targets.get(0).identifier());
        assertEquals("orders", targets.get(1).identifier());
    }

    @Test
    @DisplayName("ofTables() with single table")
    void ofTablesSingleTable() {
        var targets = ofTables("users");
        
        assertEquals(1, targets.size());
        assertEquals("users", targets.getFirst().identifier());
    }

    @Test
    @DisplayName("ofTables() with empty array returns empty list")
    void ofTablesEmpty() {
        var targets = ofTables();
        
        assertNotNull(targets);
        assertTrue(targets.isEmpty());
    }

    @Test
    @DisplayName("ofTables() throws on null identifier")
    void ofTablesNullThrows() {
        assertThrows(IllegalArgumentException.class, 
            () -> ofTables("users", null, "orders"));
    }

    @Test
    @DisplayName("ofTables() throws on blank identifier")
    void ofTablesBlankThrows() {
        assertThrows(IllegalArgumentException.class, 
            () -> ofTables("users", "", "orders"));
        
        assertThrows(IllegalArgumentException.class, 
            () -> ofTables("users", "  ", "orders"));
    }

    @Test
    @DisplayName("ofTables() with multiple tables")
    void ofTablesMultiple() {
        var targets = ofTables("t1", "t2", "t3", "t4");
        
        assertEquals(4, targets.size());
        assertEquals("t1", targets.get(0).identifier());
        assertEquals("t2", targets.get(1).identifier());
        assertEquals("t3", targets.get(2).identifier());
        assertEquals("t4", targets.get(3).identifier());
    }

    @Test
    @DisplayName("ofTables() returns mutable list")
    void ofTablesReturnsMutableList() {
        var targets = ofTables("users", "orders");
        
        // Should be able to add more targets
        targets.add(LockTarget.of("products"));
        assertEquals(3, targets.size());
    }

    @Test
    @DisplayName("Integration test - full locking clause with DSL")
    void fullLockingClause() {
        var query = select(col("*"))
            .from(tbl("users").as("u"))
            .where(col("u", "active").eq(lit(true)))
            .lockFor(update(), ofTables("u"), false, false)
            .build();
        
        assertNotNull(query.lockFor());
        assertEquals(LockMode.UPDATE, query.lockFor().mode());
        assertEquals(1, query.lockFor().ofTables().size());
    }
}
