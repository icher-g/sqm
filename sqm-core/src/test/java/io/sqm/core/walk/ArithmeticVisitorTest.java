package io.sqm.core.walk;


import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for arithmetic-related visitor methods on {@link RecursiveNodeVisitor}.
 *
 * <p>These tests verify that:
 * <ul>
 *     <li>The correct {@code visitXxxArithmeticExpr} method is invoked
 *         when visiting a corresponding arithmetic node.</li>
 *     <li>Nested arithmetic expressions are traversed recursively.</li>
 * </ul>
 */
class ArithmeticVisitorTest {

    @Test
    void visitAddArithmeticExpr_is_called_for_AddArithmeticExpr() {
        Expression expr = AddArithmeticExpr.of(lit(1), lit(2));

        RecordingArithmeticVisitor visitor = new RecordingArithmeticVisitor();
        expr.accept(visitor);

        assertTrue(visitor.addVisited);
        assertFalse(visitor.subVisited);
        assertFalse(visitor.mulVisited);
        assertFalse(visitor.divVisited);
        assertFalse(visitor.modVisited);
        assertFalse(visitor.negVisited);
        assertEquals(1, visitor.visitedOrder.size());
        assertInstanceOf(AddArithmeticExpr.class, visitor.visitedOrder.getFirst());
    }

    // -------------------------------------------------------------------------
    // Single-node tests: ensure correct visit method is triggered
    // -------------------------------------------------------------------------

    @Test
    void visitSubArithmeticExpr_is_called_for_SubArithmeticExpr() {
        Expression expr = SubArithmeticExpr.of(lit(3), lit(1));

        RecordingArithmeticVisitor visitor = new RecordingArithmeticVisitor();
        expr.accept(visitor);

        assertFalse(visitor.addVisited);
        assertTrue(visitor.subVisited);
        assertFalse(visitor.mulVisited);
        assertFalse(visitor.divVisited);
        assertFalse(visitor.modVisited);
        assertFalse(visitor.negVisited);
        assertEquals(1, visitor.visitedOrder.size());
        assertInstanceOf(SubArithmeticExpr.class, visitor.visitedOrder.getFirst());
    }

    @Test
    void visitMulArithmeticExpr_is_called_for_MulArithmeticExpr() {
        Expression expr = MulArithmeticExpr.of(lit(2), lit(4));

        RecordingArithmeticVisitor visitor = new RecordingArithmeticVisitor();
        expr.accept(visitor);

        assertFalse(visitor.addVisited);
        assertFalse(visitor.subVisited);
        assertTrue(visitor.mulVisited);
        assertFalse(visitor.divVisited);
        assertFalse(visitor.modVisited);
        assertFalse(visitor.negVisited);
        assertEquals(1, visitor.visitedOrder.size());
        assertInstanceOf(MulArithmeticExpr.class, visitor.visitedOrder.getFirst());
    }

    @Test
    void visitDivArithmeticExpr_is_called_for_DivArithmeticExpr() {
        Expression expr = DivArithmeticExpr.of(lit(8), lit(2));

        RecordingArithmeticVisitor visitor = new RecordingArithmeticVisitor();
        expr.accept(visitor);

        assertFalse(visitor.addVisited);
        assertFalse(visitor.subVisited);
        assertFalse(visitor.mulVisited);
        assertTrue(visitor.divVisited);
        assertFalse(visitor.modVisited);
        assertFalse(visitor.negVisited);
        assertEquals(1, visitor.visitedOrder.size());
        assertInstanceOf(DivArithmeticExpr.class, visitor.visitedOrder.getFirst());
    }

    @Test
    void visitModArithmeticExpr_is_called_for_ModArithmeticExpr() {
        Expression expr = ModArithmeticExpr.of(lit(5), lit(2));

        RecordingArithmeticVisitor visitor = new RecordingArithmeticVisitor();
        expr.accept(visitor);

        assertFalse(visitor.addVisited);
        assertFalse(visitor.subVisited);
        assertFalse(visitor.mulVisited);
        assertFalse(visitor.divVisited);
        assertTrue(visitor.modVisited);
        assertFalse(visitor.negVisited);
        assertEquals(1, visitor.visitedOrder.size());
        assertInstanceOf(ModArithmeticExpr.class, visitor.visitedOrder.getFirst());
    }

    @Test
    void visitNegativeArithmeticExpr_is_called_for_NegativeArithmeticExpr() {
        Expression expr = NegativeArithmeticExpr.of(lit(10));

        RecordingArithmeticVisitor visitor = new RecordingArithmeticVisitor();
        expr.accept(visitor);

        assertFalse(visitor.addVisited);
        assertFalse(visitor.subVisited);
        assertFalse(visitor.mulVisited);
        assertFalse(visitor.divVisited);
        assertFalse(visitor.modVisited);
        assertTrue(visitor.negVisited);
        assertEquals(1, visitor.visitedOrder.size());
        assertInstanceOf(NegativeArithmeticExpr.class, visitor.visitedOrder.getFirst());
    }

    @Test
    void visitPowerArithmeticExpr_is_called_for_PowerArithmeticExpr() {
        Expression expr = PowerArithmeticExpr.of(lit(2), lit(3));

        RecordingArithmeticVisitor visitor = new RecordingArithmeticVisitor();
        expr.accept(visitor);

        assertFalse(visitor.addVisited);
        assertFalse(visitor.subVisited);
        assertFalse(visitor.mulVisited);
        assertFalse(visitor.divVisited);
        assertFalse(visitor.modVisited);
        assertFalse(visitor.negVisited);
        assertTrue(visitor.powVisited);
        assertEquals(1, visitor.visitedOrder.size());
        assertInstanceOf(PowerArithmeticExpr.class, visitor.visitedOrder.getFirst());
    }

    @Test
    void recursiveNodeVisitor_traverses_nested_arithmetic_expressions() {
        // Expression: -( (1 + 2) * (3 - 4) ) % (2 ^ 3)
        Expression innerAdd = AddArithmeticExpr.of(lit(1), lit(2));
        Expression innerSub = SubArithmeticExpr.of(lit(3), lit(4));
        Expression mul = MulArithmeticExpr.of(innerAdd, innerSub);
        Expression neg = NegativeArithmeticExpr.of(mul);
        Expression pow = PowerArithmeticExpr.of(lit(2), lit(3));
        Expression root = ModArithmeticExpr.of(neg, pow);

        RecordingArithmeticVisitor visitor = new RecordingArithmeticVisitor();
        root.accept(visitor);

        // All arithmetic node types that appear in the tree must be visited
        assertTrue(visitor.addVisited);
        assertTrue(visitor.subVisited);
        assertTrue(visitor.mulVisited);
        assertTrue(visitor.modVisited);
        assertTrue(visitor.negVisited);
        assertTrue(visitor.powVisited);
        // No division in this tree
        assertFalse(visitor.divVisited);

        // We expect 6 arithmetic nodes in total: add, sub, mul, neg, mod, pow
        assertEquals(6, visitor.visitedOrder.size());
        assertTrue(visitor.visitedOrder.stream().anyMatch(AddArithmeticExpr.class::isInstance));
        assertTrue(visitor.visitedOrder.stream().anyMatch(SubArithmeticExpr.class::isInstance));
        assertTrue(visitor.visitedOrder.stream().anyMatch(MulArithmeticExpr.class::isInstance));
        assertTrue(visitor.visitedOrder.stream().anyMatch(ModArithmeticExpr.class::isInstance));
        assertTrue(visitor.visitedOrder.stream().anyMatch(NegativeArithmeticExpr.class::isInstance));
        assertTrue(visitor.visitedOrder.stream().anyMatch(PowerArithmeticExpr.class::isInstance));
    }

    // -------------------------------------------------------------------------
    // Recursive traversal: nested arithmetic expressions
    // -------------------------------------------------------------------------

    /**
     * A test visitor that records which arithmetic node types were visited.
     */
    private static final class RecordingArithmeticVisitor extends RecursiveNodeVisitor<Void> {

        final List<ArithmeticExpr> visitedOrder = new ArrayList<>();
        boolean addVisited;
        boolean subVisited;
        boolean mulVisited;
        boolean divVisited;
        boolean modVisited;
        boolean negVisited;
        boolean powVisited;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitAddArithmeticExpr(AddArithmeticExpr expr) {
            addVisited = true;
            visitedOrder.add(expr);
            return super.visitAddArithmeticExpr(expr);
        }

        @Override
        public Void visitSubArithmeticExpr(SubArithmeticExpr expr) {
            subVisited = true;
            visitedOrder.add(expr);
            return super.visitSubArithmeticExpr(expr);
        }

        @Override
        public Void visitMulArithmeticExpr(MulArithmeticExpr expr) {
            mulVisited = true;
            visitedOrder.add(expr);
            return super.visitMulArithmeticExpr(expr);
        }

        @Override
        public Void visitDivArithmeticExpr(DivArithmeticExpr expr) {
            divVisited = true;
            visitedOrder.add(expr);
            return super.visitDivArithmeticExpr(expr);
        }

        @Override
        public Void visitModArithmeticExpr(ModArithmeticExpr expr) {
            modVisited = true;
            visitedOrder.add(expr);
            return super.visitModArithmeticExpr(expr);
        }

        @Override
        public Void visitNegativeArithmeticExpr(NegativeArithmeticExpr expr) {
            negVisited = true;
            visitedOrder.add(expr);
            return super.visitNegativeArithmeticExpr(expr);
        }

        @Override
        public Void visitPowerArithmeticExpr(PowerArithmeticExpr expr) {
            powVisited = true;
            visitedOrder.add(expr);
            return super.visitPowerArithmeticExpr(expr);
        }
    }
}

