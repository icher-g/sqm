package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhenThenTest {

    @Test
    void of() {
        var p = Expression.column("c").eq(1);
        var e = Expression.literal(1);
        var whenThen = WhenThen.of(p, e);
        assertEquals("c", whenThen.when().asComparison().orElseThrow().lhs().asColumn().orElseThrow().name());
        assertEquals(1, whenThen.then().asLiteral().orElseThrow().value());
    }

    @Test
    void when() {
        var p = Expression.column("c").eq(1);
        var e = Expression.literal(1);
        var whenThen = WhenThen.when(p).then(e);
        assertEquals("c", whenThen.when().asComparison().orElseThrow().lhs().asColumn().orElseThrow().name());
        assertEquals(1, whenThen.then().asLiteral().orElseThrow().value());
    }

    @Test
    void then() {
        var p = Expression.column("c").eq(1);
        var whenThen = WhenThen.when(p).then(1);
        assertEquals("c", whenThen.when().asComparison().orElseThrow().lhs().asColumn().orElseThrow().name());
        assertEquals(1, whenThen.then().asLiteral().orElseThrow().value());
    }
}