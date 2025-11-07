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
        assertEquals(JoinKind.INNER, join.asOn().orElseThrow().kind());
        assertEquals("t", join.asOn().orElseThrow().right().asTable().orElseThrow().name());
        assertEquals("c", join.asOn().orElseThrow().on().asComparison().orElseThrow().lhs().asColumn().orElseThrow().name());
    }

    @Test
    void left() {
        Predicate p = Expression.column("c").gt(1);
        Join join = Join.left(TableRef.table("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.LEFT, join.asOn().orElseThrow().kind());
        assertEquals("t", join.asOn().orElseThrow().right().asTable().orElseThrow().name());
        assertEquals("c", join.asOn().orElseThrow().on().asComparison().orElseThrow().lhs().asColumn().orElseThrow().name());
    }

    @Test
    void right() {
        Predicate p = Expression.column("c").gt(1);
        Join join = Join.right(TableRef.table("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.RIGHT, join.asOn().orElseThrow().kind());
        assertEquals("t", join.asOn().orElseThrow().right().asTable().orElseThrow().name());
        assertEquals("c", join.asOn().orElseThrow().on().asComparison().orElseThrow().lhs().asColumn().orElseThrow().name());
    }

    @Test
    void full() {
        Predicate p = Expression.column("c").gt(1);
        Join join = Join.full(TableRef.table("t")).on(p);
        assertInstanceOf(OnJoin.class, join);
        assertEquals(JoinKind.FULL, join.asOn().orElseThrow().kind());
        assertEquals("t", join.asOn().orElseThrow().right().asTable().orElseThrow().name());
        assertEquals("c", join.asOn().orElseThrow().on().asComparison().orElseThrow().lhs().asColumn().orElseThrow().name());
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
    void asOn() {
        assertTrue(Join.right(TableRef.table("t")).asOn().isPresent());
        assertFalse(Join.cross("t").asOn().isPresent());
    }

    @Test
    void asCross() {
        assertFalse(Join.right(TableRef.table("t")).asCross().isPresent());
        assertTrue(Join.cross("t").asCross().isPresent());
    }

    @Test
    void asUsing() {
        assertFalse(Join.right(TableRef.table("t")).asUsing().isPresent());
        assertTrue(Join.using(TableRef.table("t")).asUsing().isPresent());
    }

    @Test
    void asNatural() {
        assertFalse(Join.right(TableRef.table("t")).asNatural().isPresent());
        assertTrue(Join.natural(TableRef.table("t")).asNatural().isPresent());
    }
}