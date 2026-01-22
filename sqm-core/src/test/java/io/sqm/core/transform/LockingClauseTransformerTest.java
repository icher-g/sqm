package io.sqm.core.transform;

import io.sqm.core.LockMode;
import io.sqm.core.LockingClause;
import io.sqm.core.Node;
import io.sqm.core.SelectQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LockingClause Transformer Tests")
class LockingClauseTransformerTest {

    @Test
    @DisplayName("Transformer preserves locking clause by default")
    void transformerPreservesLockingClause() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var transformer = new RecursiveNodeTransformer() {};
        
        var result = transformer.visitLockingClause(clause);
        
        assertSame(clause, result);
    }

    @Test
    @DisplayName("Transformer can modify locking clause")
    void transformerCanModifyLockingClause() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var transformer = new LockModeTransformer();
        
        var result = (LockingClause) transformer.visitLockingClause(clause);
        
        assertNotSame(clause, result);
        assertEquals(LockMode.SHARE, result.mode());
    }

    @Test
    @DisplayName("Transformer can add NOWAIT flag")
    void transformerCanAddNowait() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var transformer = new AddNowaitTransformer();
        
        var result = (LockingClause) transformer.visitLockingClause(clause);
        
        assertTrue(result.nowait());
        assertFalse(result.skipLocked());
    }

    @Test
    @DisplayName("Transformer applies to query with locking clause")
    void transformerAppliedToQuery() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(update(), List.of(), false, false);
        
        var transformer = new LockModeTransformer();
        var transformedQuery = (SelectQuery) transformer.transform(query);
        
        assertNotNull(transformedQuery.lockFor());
        assertEquals(LockMode.SHARE, transformedQuery.lockFor().mode());
    }

    @Test
    @DisplayName("Transformer preserves query without locking clause")
    void transformerPreservesQueryWithoutLocking() {
        var query = select(col("*"))
            .from(tbl("users"));
        
        var transformer = new LockModeTransformer();
        var transformedQuery = (SelectQuery) transformer.transform(query);
        
        assertNull(transformedQuery.lockFor());
    }

    private static class LockModeTransformer extends RecursiveNodeTransformer {
        @Override
        public Node visitLockingClause(LockingClause clause) {
            // Transform UPDATE to SHARE
            if (clause.mode() == LockMode.UPDATE) {
                return LockingClause.of(LockMode.SHARE, clause.ofTables(), 
                    clause.nowait(), clause.skipLocked());
            }
            return clause;
        }
    }

    private static class AddNowaitTransformer extends RecursiveNodeTransformer {
        @Override
        public Node visitLockingClause(LockingClause clause) {
            // Add NOWAIT if not present
            if (!clause.nowait() && !clause.skipLocked()) {
                return LockingClause.of(clause.mode(), clause.ofTables(), 
                    true, false);
            }
            return clause;
        }
    }
}
