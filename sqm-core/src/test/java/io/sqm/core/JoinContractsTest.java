package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class JoinContractsTest {

    @Test
    void on_join() {
        var p = ComparisonPredicate.of(Expression.column("t", "c"), ComparisonOperator.EQ, Expression.literal(1));
        var j = OnJoin.of(TableRef.table("t"), JoinKind.INNER, p);

        assertEquals("t", j.right().asTable().map(Table::name).orElseThrow());
        assertEquals(JoinKind.INNER, j.kind());
        assertInstanceOf(ComparisonPredicate.class, j.on());
    }

    @Test
    void cross_join() {
        var j = CrossJoin.of("t");

        assertEquals("t", j.right().asTable().map(Table::name).orElseThrow());
    }

    @Test
    void natural_join() {
        var j = NaturalJoin.of("t");

        assertEquals("t", j.right().asTable().map(Table::name).orElseThrow());
    }

    @Test
    void using_join() {
        var j = UsingJoin.of(TableRef.table("t"), "c1");

        assertEquals("t", j.right().asTable().map(Table::name).orElseThrow());
        assertEquals("c1", j.usingColumns().get(0));
    }
}
