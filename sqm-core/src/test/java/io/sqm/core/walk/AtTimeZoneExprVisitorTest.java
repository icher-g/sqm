package io.sqm.core.walk;

import io.sqm.core.AtTimeZoneExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.LiteralExpr;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AtTimeZoneExpr} visitor support.
 * <p>
 * These tests verify that:
 * <ul>
 *     <li>The {@code visitAtTimeZoneExpr} method is correctly invoked when visiting an {@link AtTimeZoneExpr}</li>
 *     <li>Child expressions (timestamp and timezone) are recursively visited</li>
 *     <li>Information can be collected from nested AT TIME ZONE expressions</li>
 * </ul>
 */
class AtTimeZoneExprVisitorTest {

    @Test
    void visitAtTimeZoneExpr_isInvoked() {
        AtTimeZoneExpr expr = AtTimeZoneExpr.of(col("created_at"), lit("UTC"));

        VisitRecorder visitor = new VisitRecorder();
        expr.accept(visitor);

        assertTrue(visitor.visitedAtTimeZone);
        assertEquals(3, visitor.visitedExprCount);  // AtTimeZone + column + literal
    }

    @Test
    void visitAtTimeZoneExpr_recursivelyVisitsChildren() {
        AtTimeZoneExpr expr = AtTimeZoneExpr.of(col("t", "ts"), col("tz_offset"));

        ChildCollector visitor = new ChildCollector();
        expr.accept(visitor);

        assertEquals(2, visitor.columnExpressions.size());
        assertTrue(visitor.columnExpressions.contains("t.ts"));
        assertTrue(visitor.columnExpressions.contains("tz_offset"));
    }

    @Test
    void visitAtTimeZoneExpr_withLiteralTimezone() {
        AtTimeZoneExpr expr = AtTimeZoneExpr.of(
            col("event_time"),
            lit("America/New_York")
        );

        ChildCollector visitor = new ChildCollector();
        expr.accept(visitor);

        assertEquals(1, visitor.columnExpressions.size());
        assertEquals(1, visitor.literalExpressions.size());
    }

    @Test
    void visitAtTimeZoneExpr_withNestedExpressions() {
        // Create: (ts_col AT TIME ZONE 'UTC') AT TIME ZONE tz_param
        AtTimeZoneExpr inner = AtTimeZoneExpr.of(col("ts"), lit("UTC"));
        AtTimeZoneExpr outer = AtTimeZoneExpr.of(inner, param("tz"));

        ChildCollector visitor = new ChildCollector();
        outer.accept(visitor);

        assertEquals(1, visitor.columnExpressions.size());
        assertEquals(1, visitor.literalExpressions.size());
        assertEquals(2, visitor.atTimeZoneCount);
    }

    /**
     * Helper visitor that records whether visitAtTimeZoneExpr was called.
     */
    private static class VisitRecorder extends RecursiveNodeVisitor<Void> {
        boolean visitedAtTimeZone = false;
        int visitedExprCount = 0;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitAtTimeZoneExpr(AtTimeZoneExpr expr) {
            visitedAtTimeZone = true;
            visitedExprCount++;
            return super.visitAtTimeZoneExpr(expr);
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            visitedExprCount++;
            return super.visitColumnExpr(c);
        }

        @Override
        public Void visitLiteralExpr(LiteralExpr l) {
            visitedExprCount++;
            return super.visitLiteralExpr(l);
        }
    }

    /**
     * Helper visitor that collects information from visited expressions.
     */
    private static class ChildCollector extends RecursiveNodeVisitor<Void> {
        final List<String> columnExpressions = new ArrayList<>();
        final List<Object> literalExpressions = new ArrayList<>();
        int atTimeZoneCount = 0;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitAtTimeZoneExpr(AtTimeZoneExpr expr) {
            atTimeZoneCount++;
            return super.visitAtTimeZoneExpr(expr);
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            String qualified = c.tableAlias() == null ? c.name().value() : c.tableAlias().value() + "." + c.name().value();
            columnExpressions.add(qualified);
            return super.visitColumnExpr(c);
        }

        @Override
        public Void visitLiteralExpr(LiteralExpr l) {
            literalExpressions.add(l.value());
            return super.visitLiteralExpr(l);
        }
    }
}

