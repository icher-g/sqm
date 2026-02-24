package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelectItemTest {

    @Test
    void star() {
        assertInstanceOf(StarSelectItem.class, SelectItem.star());
    }

    @Test
    void maybeExpr() {
        var expr = Expression.literal(1);
        assertTrue(SelectItem.expr(expr).<Boolean>matchSelectItem().expr(e -> true).orElse(false));
        assertFalse(SelectItem.star().<Boolean>matchSelectItem().expr(e -> true).orElse(false));
    }

    @Test
    void maybeStar() {
        assertTrue(SelectItem.star().<Boolean>matchSelectItem().star(s -> true).orElse(false));
        assertFalse(SelectItem.star(Identifier.of("t")).<Boolean>matchSelectItem().star(s -> true).orElse(false));
    }

    @Test
    void maybeQualifiedStar() {
        assertTrue(SelectItem.star(Identifier.of("t")).<Boolean>matchSelectItem().qualifiedStar(qs -> true).orElse(false));
        assertFalse(SelectItem.star().<Boolean>matchSelectItem().qualifiedStar(qs -> true).orElse(false));
    }
}
