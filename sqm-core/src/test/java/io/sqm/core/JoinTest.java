package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.*;

class JoinTest {

    @Test
    void inner() {
        Predicate p = col("c").gt(1);
        Join join = Join.inner(tbl("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.INNER, join.matchJoin().on(o -> o.kind()).orElse(null));
        assertEquals("t", join.matchJoin()
            .on(o -> o.right().matchTableRef()
                .table(t -> t.name().value())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c", join.matchJoin()
            .on(o -> o.on().matchPredicate()
                .comparison(cmp -> cmp.lhs().matchExpression()
                    .column(c -> c.name().value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void left() {
        Predicate p = col("c").gt(1);
        Join join = Join.left(tbl("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.LEFT, join.matchJoin().on(o -> o.kind()).orElse(null));
        assertEquals("t", join.matchJoin()
            .on(o -> o.right().matchTableRef()
                .table(t -> t.name().value())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c", join.matchJoin()
            .on(o -> o.on().matchPredicate()
                .comparison(cmp -> cmp.lhs().matchExpression()
                    .column(c -> c.name().value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void right() {
        Predicate p = col("c").gt(1);
        Join join = Join.right(tbl("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.RIGHT, join.matchJoin().on(o -> o.kind()).orElse(null));
        assertEquals("t", join.matchJoin()
            .on(o -> o.right().matchTableRef()
                .table(t -> t.name().value())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c", join.matchJoin()
            .on(o -> o.on().matchPredicate()
                .comparison(cmp -> cmp.lhs().matchExpression()
                    .column(c -> c.name().value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void full() {
        Predicate p = col("c").gt(1);
        Join join = Join.full(tbl("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.FULL, join.matchJoin().on(o -> o.kind()).orElse(null));
        assertEquals("t", join.matchJoin()
            .on(o -> o.right().matchTableRef()
                .table(t -> t.name().value())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c", join.matchJoin()
            .on(o -> o.on().matchPredicate()
                .comparison(cmp -> cmp.lhs().matchExpression()
                    .column(c -> c.name().value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void cross() {
        assertInstanceOf(CrossJoin.class, Join.cross(tbl("t")));
        assertInstanceOf(CrossJoin.class, Join.cross(tbl("t")));
        assertInstanceOf(CrossJoin.class, Join.cross(tbl("s", "t")));
    }

    @Test
    void using() {
        assertInstanceOf(UsingJoin.class, Join.inner(tbl("t")).using("a"));
        assertInstanceOf(UsingJoin.class, Join.inner(tbl("t")).using(List.of(Identifier.of("a"))));
    }

    @Test
    void natural() {
        assertInstanceOf(NaturalJoin.class, Join.natural(tbl("t")));
        assertInstanceOf(NaturalJoin.class, Join.natural(tbl("t")));
        assertInstanceOf(NaturalJoin.class, Join.natural(tbl("s", "t")));
    }

    @Test
    void maybeOn() {
        assertTrue(Join.right(tbl("t")).<Boolean>matchJoin().on(o -> true).orElse(false));
        assertFalse(Join.cross(tbl("t")).<Boolean>matchJoin().on(o -> true).orElse(false));
    }

    @Test
    void maybeCross() {
        assertFalse(Join.right(tbl("t")).<Boolean>matchJoin().cross(j -> true).orElse(false));
        assertTrue(Join.cross(tbl("t")).<Boolean>matchJoin().cross(j -> true).orElse(false));
    }

    @Test
    void maybeUsing() {
        assertFalse(Join.right(tbl("t")).<Boolean>matchJoin().using(j -> true).orElse(false));
        assertTrue(Join.inner(tbl("t")).using("a").<Boolean>matchJoin().using(j -> true).orElse(false));
    }

    @Test
    void maybeNatural() {
        assertFalse(Join.right(tbl("t")).<Boolean>matchJoin().natural(j -> true).orElse(false));
        assertTrue(Join.natural(tbl("t")).<Boolean>matchJoin().natural(j -> true).orElse(false));
    }
}