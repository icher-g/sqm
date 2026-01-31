package io.sqm.core.walk;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtendedLiteralVisitorTest {

    @Test
    void visits_all_typed_literal_nodes_via_default_hooks() {
        var visitor = new CountingVisitor();

        DateLiteralExpr.of("2020-01-01").accept(visitor);
        TimeLiteralExpr.of("10:11:12").accept(visitor);
        TimestampLiteralExpr.of("2020-01-01 00:00:00").accept(visitor);
        IntervalLiteralExpr.of("1", "DAY").accept(visitor);
        BitStringLiteralExpr.of("1010").accept(visitor);
        HexStringLiteralExpr.of("FF").accept(visitor);
        EscapeStringLiteralExpr.of("it\\'s").accept(visitor);
        DollarStringLiteralExpr.of("tag", "value").accept(visitor);

        assertEquals(8, visitor.count);
    }

    private static final class CountingVisitor extends RecursiveNodeVisitor<Void> {
        private int count = 0;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitLiteralExpr(LiteralExpr l) {
            count++;
            return super.visitLiteralExpr(l);
        }
    }
}
