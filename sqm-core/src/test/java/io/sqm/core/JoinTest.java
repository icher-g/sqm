package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinTest {

    @Test
    void inner() {
        Predicate p = Expression.column("c").gt(1);
        Join join = Join.join(TableRef.table("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.INNER, join.matchJoin().on(o -> o.kind()).orElse(null));
        assertEquals("t", join.matchJoin()
            .on(o -> o.right().matchTableRef()
                .table(t -> t.name())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c", join.matchJoin()
            .on(o -> o.on().matchPredicate()
                .comparison(cmp -> cmp.lhs().matchExpression()
                    .column(c -> c.name())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void left() {
        Predicate p = Expression.column("c").gt(1);
        Join join = Join.left(TableRef.table("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.LEFT, join.matchJoin().on(o -> o.kind()).orElse(null));
        assertEquals("t", join.matchJoin()
            .on(o -> o.right().matchTableRef()
                .table(t -> t.name())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c", join.matchJoin()
            .on(o -> o.on().matchPredicate()
                .comparison(cmp -> cmp.lhs().matchExpression()
                    .column(c -> c.name())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void right() {
        Predicate p = Expression.column("c").gt(1);
        Join join = Join.right(TableRef.table("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.RIGHT, join.matchJoin().on(o -> o.kind()).orElse(null));
        assertEquals("t", join.matchJoin()
            .on(o -> o.right().matchTableRef()
                .table(t -> t.name())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c", join.matchJoin()
            .on(o -> o.on().matchPredicate()
                .comparison(cmp -> cmp.lhs().matchExpression()
                    .column(c -> c.name())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void full() {
        Predicate p = Expression.column("c").gt(1);
        Join join = Join.full(TableRef.table("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.FULL, join.matchJoin().on(o -> o.kind()).orElse(null));
        assertEquals("t", join.matchJoin()
            .on(o -> o.right().matchTableRef()
                .table(t -> t.name())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c", join.matchJoin()
            .on(o -> o.on().matchPredicate()
                .comparison(cmp -> cmp.lhs().matchExpression()
                    .column(c -> c.name())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void cross() {
        assertInstanceOf(CrossJoin.class, Join.cross(TableRef.table("t")));
        assertInstanceOf(CrossJoin.class, Join.cross("t"));
        assertInstanceOf(CrossJoin.class, Join.cross("s", "t"));
    }

    @Test
    void using() {
        assertInstanceOf(UsingJoin.class, Join.using(TableRef.table("t"), "a"));
        assertInstanceOf(UsingJoin.class, Join.using(TableRef.table("t"), List.of("a")));
    }

    @Test
    void natural() {
        assertInstanceOf(NaturalJoin.class, Join.natural(TableRef.table("t")));
        assertInstanceOf(NaturalJoin.class, Join.natural("t"));
        assertInstanceOf(NaturalJoin.class, Join.natural("s", "t"));
    }

    @Test
    void maybeOn() {
        assertTrue(Join.right(TableRef.table("t")).<Boolean>matchJoin().on(o -> true).orElse(false));
        assertFalse(Join.cross("t").<Boolean>matchJoin().on(o -> true).orElse(false));
    }

    @Test
    void maybeCross() {
        assertFalse(Join.right(TableRef.table("t")).<Boolean>matchJoin().cross(j -> true).orElse(false));
        assertTrue(Join.cross("t").<Boolean>matchJoin().cross(j -> true).orElse(false));
    }

    @Test
    void maybeUsing() {
        assertFalse(Join.right(TableRef.table("t")).<Boolean>matchJoin().using(j -> true).orElse(false));
        assertTrue(Join.using(TableRef.table("t")).<Boolean>matchJoin().using(j -> true).orElse(false));
    }

    @Test
    void maybeNatural() {
        assertFalse(Join.right(TableRef.table("t")).<Boolean>matchJoin().natural(j -> true).orElse(false));
        assertTrue(Join.natural(TableRef.table("t")).<Boolean>matchJoin().natural(j -> true).orElse(false));
    }
}