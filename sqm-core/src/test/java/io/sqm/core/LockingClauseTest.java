package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.ofTables;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LockingClause Tests")
class LockingClauseTest {

    @Test
    @DisplayName("Create simple FOR UPDATE clause")
    void createForUpdate() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        
        assertNotNull(clause);
        assertEquals(LockMode.UPDATE, clause.mode());
        assertTrue(clause.ofTables().isEmpty());
        assertFalse(clause.nowait());
        assertFalse(clause.skipLocked());
    }

    @Test
    @DisplayName("Create FOR UPDATE with NOWAIT")
    void createWithNowait() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), true, false);
        
        assertNotNull(clause);
        assertEquals(LockMode.UPDATE, clause.mode());
        assertTrue(clause.nowait());
        assertFalse(clause.skipLocked());
    }

    @Test
    @DisplayName("Create FOR UPDATE with SKIP LOCKED")
    void createWithSkipLocked() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, true);
        
        assertNotNull(clause);
        assertEquals(LockMode.UPDATE, clause.mode());
        assertFalse(clause.nowait());
        assertTrue(clause.skipLocked());
    }

    @Test
    @DisplayName("NOWAIT and SKIP LOCKED are mutually exclusive")
    void nowaitAndSkipLockedMutuallyExclusive() {
        assertThrows(IllegalArgumentException.class, 
            () -> LockingClause.of(LockMode.UPDATE, List.of(), true, true));
    }

    @Test
    @DisplayName("Create FOR UPDATE OF with table targets")
    void createWithOfTables() {
        var targets = ofTables("users", "orders");
        var clause = LockingClause.of(LockMode.UPDATE, targets, false, false);
        
        assertNotNull(clause);
        assertEquals(2, clause.ofTables().size());
        assertEquals("users", clause.ofTables().get(0).identifier().value());
        assertEquals("orders", clause.ofTables().get(1).identifier().value());
    }

    @Test
    @DisplayName("Create FOR SHARE clause")
    void createForShare() {
        var clause = LockingClause.of(LockMode.SHARE, List.of(), false, false);
        
        assertNotNull(clause);
        assertEquals(LockMode.SHARE, clause.mode());
    }

    @Test
    @DisplayName("Create FOR NO KEY UPDATE clause")
    void createForNoKeyUpdate() {
        var clause = LockingClause.of(LockMode.NO_KEY_UPDATE, List.of(), false, false);
        
        assertNotNull(clause);
        assertEquals(LockMode.NO_KEY_UPDATE, clause.mode());
    }

    @Test
    @DisplayName("Create FOR KEY SHARE clause")
    void createForKeyShare() {
        var clause = LockingClause.of(LockMode.KEY_SHARE, List.of(), false, false);
        
        assertNotNull(clause);
        assertEquals(LockMode.KEY_SHARE, clause.mode());
    }

    @Test
    @DisplayName("Null ofTables list becomes empty list")
    void nullOfTablesBecomesEmpty() {
        var clause = LockingClause.of(LockMode.UPDATE, null, false, false);
        
        assertNotNull(clause.ofTables());
        assertTrue(clause.ofTables().isEmpty());
    }

    @Test
    @DisplayName("ofTables list is copied and immutable")
    void ofTablesIsCopied() {
        var mutableList = new java.util.ArrayList<LockTarget>();
        mutableList.add(LockTarget.of(Identifier.of("users")));
        var clause = LockingClause.of(LockMode.UPDATE, mutableList, false, false);
        
        // Verify it's a copy by checking that modifying original doesn't affect clause
        mutableList.add(LockTarget.of(Identifier.of("orders")));
        assertEquals(1, clause.ofTables().size());
        
        // Verify immutability
        assertThrows(UnsupportedOperationException.class, 
            () -> clause.ofTables().add(LockTarget.of(Identifier.of("products"))));
    }

    @Test
    @DisplayName("Accept visitor")
    void acceptVisitor() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var result = clause.accept(new TestVisitor());
        
        assertEquals("LockingClause", result);
    }

    @Test
    @DisplayName("LockingClause equality works")
    void equalityWorks() {
        var clause1 = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var clause2 = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var clause3 = LockingClause.of(LockMode.UPDATE, List.of(), true, false);
        
        assertEquals(clause1, clause2);
        assertNotEquals(clause1, clause3);
        assertEquals(clause1.hashCode(), clause2.hashCode());
    }

    @Test
    @DisplayName("Complex FOR UPDATE OF with NOWAIT")
    void complexClause() {
        var targets = List.of(LockTarget.of(Identifier.of("t1")), LockTarget.of(Identifier.of("t2")));
        var clause = LockingClause.of(LockMode.UPDATE, targets, true, false);
        
        assertNotNull(clause);
        assertEquals(LockMode.UPDATE, clause.mode());
        assertEquals(2, clause.ofTables().size());
        assertTrue(clause.nowait());
        assertFalse(clause.skipLocked());
    }

    private static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<String> {
        @Override
        protected String defaultResult() {
            return null;
        }

        @Override
        public String visitLockingClause(LockingClause clause) {
            return "LockingClause";
        }
    }
}
