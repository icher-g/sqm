package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class JoinContractsTest {

    @Test
    void on_join() {
        var p = ComparisonPredicate.of(Expression.column("t", "c"), ComparisonOperator.EQ, Expression.literal(1));
        var j = OnJoin.of(TableRef.table("t"), JoinKind.INNER, p);
        var name = j.right().matchTableRef()
                    .table(t -> t.name())
                    .orElse(null);
        assertEquals("t", name);
        assertEquals(JoinKind.INNER, j.kind());
        assertInstanceOf(ComparisonPredicate.class, j.on());
    }

    @Test
    void cross_join() {
        var j = CrossJoin.of("t");
        var name = j.right().matchTableRef()
                    .table(t -> t.name())
                    .orElse(null);
        assertEquals("t", name);
    }

    @Test
    void natural_join() {
        var j = NaturalJoin.of("t");
        var name = j.right().matchTableRef()
                    .table(t -> t.name())
                    .orElse(null);
        assertEquals("t", name);
    }

    @Test
    void using_join() {
        var j = UsingJoin.of(TableRef.table("t"), JoinKind.INNER, "c1");
        var name = j.right().matchTableRef()
                    .table(t -> t.name())
                    .orElse(null);
        assertEquals("t", name);
        assertEquals("c1", j.usingColumns().getFirst());
    }
}
