package io.sqm.core.walk;

import io.sqm.core.LockMode;
import io.sqm.core.LockTarget;
import io.sqm.core.LockingClause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LockingClause Visitor Tests")
class LockingClauseVisitorTest {

    @Test
    @DisplayName("Visitor visits locking clause")
    void visitorVisitsLockingClause() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, false);
        var visitor = new CountingVisitor();
        
        clause.accept(visitor);
        
        assertEquals(1, visitor.lockingClauseCount);
    }

    @Test
    @DisplayName("Visitor in SELECT query visits locking clause")
    void visitorInSelectQuery() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(update(), List.of(), false, false);
        
        var visitor = new CountingVisitor();
        query.accept(visitor);
        
        assertEquals(1, visitor.lockingClauseCount);
    }

    @Test
    @DisplayName("Visitor extracts lock mode")
    void visitorExtractsLockMode() {
        var clause = LockingClause.of(LockMode.SHARE, List.of(), false, false);
        var visitor = new LockModeCollector();
        
        clause.accept(visitor);
        
        assertEquals(1, visitor.modes.size());
        assertEquals(LockMode.SHARE, visitor.modes.getFirst());
    }

    @Test
    @DisplayName("Visitor extracts lock targets")
    void visitorExtractsLockTargets() {
        var targets = List.of(LockTarget.of("users"), LockTarget.of("orders"));
        var clause = LockingClause.of(LockMode.UPDATE, targets, false, false);
        var visitor = new LockTargetCollector();
        
        clause.accept(visitor);
        
        assertEquals(2, visitor.targets.size());
        assertEquals("users", visitor.targets.get(0));
        assertEquals("orders", visitor.targets.get(1));
    }

    @Test
    @DisplayName("Visitor detects NOWAIT flag")
    void visitorDetectsNowait() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), true, false);
        var visitor = new FlagDetector();
        
        clause.accept(visitor);
        
        assertTrue(visitor.hasNowait);
        assertFalse(visitor.hasSkipLocked);
    }

    @Test
    @DisplayName("Visitor detects SKIP LOCKED flag")
    void visitorDetectsSkipLocked() {
        var clause = LockingClause.of(LockMode.UPDATE, List.of(), false, true);
        var visitor = new FlagDetector();
        
        clause.accept(visitor);
        
        assertFalse(visitor.hasNowait);
        assertTrue(visitor.hasSkipLocked);
    }

    @Test
    @DisplayName("Query without locking clause does not visit locking clause")
    void queryWithoutLockingClause() {
        var query = select(col("*"))
            .from(tbl("users"));
        
        var visitor = new CountingVisitor();
        query.accept(visitor);
        
        assertEquals(0, visitor.lockingClauseCount);
    }

    private static class CountingVisitor extends RecursiveNodeVisitor<Void> {
        int lockingClauseCount = 0;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitLockingClause(LockingClause clause) {
            lockingClauseCount++;
            return super.visitLockingClause(clause);
        }
    }

    private static class LockModeCollector extends RecursiveNodeVisitor<Void> {
        List<LockMode> modes = new ArrayList<>();

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitLockingClause(LockingClause clause) {
            modes.add(clause.mode());
            return super.visitLockingClause(clause);
        }
    }

    private static class LockTargetCollector extends RecursiveNodeVisitor<Void> {
        List<String> targets = new ArrayList<>();

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitLockingClause(LockingClause clause) {
            clause.ofTables().forEach(t -> targets.add(t.identifier()));
            return super.visitLockingClause(clause);
        }
    }

    private static class FlagDetector extends RecursiveNodeVisitor<Void> {
        boolean hasNowait = false;
        boolean hasSkipLocked = false;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitLockingClause(LockingClause clause) {
            hasNowait = clause.nowait();
            hasSkipLocked = clause.skipLocked();
            return super.visitLockingClause(clause);
        }
    }
}
