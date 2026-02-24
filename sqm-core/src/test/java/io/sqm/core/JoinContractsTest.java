package io.sqm.core;

import org.junit.jupiter.api.Test;
import java.util.List;

import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static io.sqm.dsl.Dsl.col;

public class JoinContractsTest {

    @Test
    void on_join() {
        var p = ComparisonPredicate.of(col("t", "c"), ComparisonOperator.EQ, Expression.literal(1));
        var j = OnJoin.of(TableRef.table(Identifier.of("t")), JoinKind.INNER, p);
        var name = j.right().matchTableRef()
                    .table(t -> t.name().value())
                    .orElse(null);
        assertEquals("t", name);
        assertEquals(JoinKind.INNER, j.kind());
        assertInstanceOf(ComparisonPredicate.class, j.on());
    }

    @Test
    void cross_join() {
        var j = CrossJoin.of(tbl("t"));
        var name = j.right().matchTableRef()
                    .table(t -> t.name().value())
                    .orElse(null);
        assertEquals("t", name);
    }

    @Test
    void natural_join() {
        var j = NaturalJoin.of(tbl("t"));
        var name = j.right().matchTableRef()
                    .table(t -> t.name().value())
                    .orElse(null);
        assertEquals("t", name);
    }

    @Test
    void using_join() {
        var j = UsingJoin.of(tbl("t"), JoinKind.INNER, List.of(Identifier.of("c1")));
        var name = j.right().matchTableRef()
                    .table(t -> t.name().value())
                    .orElse(null);
        assertEquals("t", name);
        assertEquals("c1", j.usingColumns().getFirst().value());
    }
}
