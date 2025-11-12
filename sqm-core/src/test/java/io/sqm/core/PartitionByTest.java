package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartitionByTest {

    @Test
    void of() {
        var p = PartitionBy.of(Expression.literal(1));
        assertEquals(1, p.items().size());
        assertEquals(1, p.items().getFirst().matchExpression()
                         .literal(l -> l.value())
                         .orElse(null));
        p = PartitionBy.of(List.of(Expression.literal(1)));
        assertEquals(1, p.items().size());
        assertEquals(1, p.items().getFirst().matchExpression()
                         .literal(l -> l.value())
                         .orElse(null));
    }
}