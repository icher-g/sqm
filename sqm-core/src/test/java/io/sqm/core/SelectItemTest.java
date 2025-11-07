package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelectItemTest {

    @Test
    void star() {
        assertInstanceOf(StarSelectItem.class, SelectItem.star());
    }

    @Test
    void asExpr() {
        var expr = Expression.literal(1);
        assertTrue(SelectItem.expr(expr).asExpr().isPresent());
        assertFalse(SelectItem.star().asExpr().isPresent());
    }

    @Test
    void asStar() {
        assertTrue(SelectItem.star().asStar().isPresent());
        assertFalse(SelectItem.star("t").asStar().isPresent());
    }

    @Test
    void asQualifiedStar() {
        assertTrue(SelectItem.star("t").asQualifiedStar().isPresent());
        assertFalse(SelectItem.star().asQualifiedStar().isPresent());
    }
}