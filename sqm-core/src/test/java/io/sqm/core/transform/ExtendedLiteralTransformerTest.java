package io.sqm.core.transform;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtendedLiteralTransformerTest {

    @Test
    void transforms_all_typed_literal_nodes_via_literal_hook() {
        var transformer = new CountingTransformer();

        transformer.transform(DateLiteralExpr.of("2020-01-01"));
        transformer.transform(TimeLiteralExpr.of("10:11:12"));
        transformer.transform(TimestampLiteralExpr.of("2020-01-01 00:00:00"));
        transformer.transform(IntervalLiteralExpr.of("1", "DAY"));
        transformer.transform(BitStringLiteralExpr.of("1010"));
        transformer.transform(HexStringLiteralExpr.of("FF"));
        transformer.transform(EscapeStringLiteralExpr.of("it\\'s"));
        transformer.transform(DollarStringLiteralExpr.of("tag", "value"));

        assertEquals(8, transformer.count);
    }

    private static final class CountingTransformer extends RecursiveNodeTransformer {
        private int count = 0;

        @Override
        public Node visitLiteralExpr(LiteralExpr l) {
            count++;
            return super.visitLiteralExpr(l);
        }
    }
}
