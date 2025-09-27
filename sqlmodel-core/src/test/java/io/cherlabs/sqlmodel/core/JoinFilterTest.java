package io.cherlabs.sqlmodel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JoinFilterTest {

    @Test
    void constructs_with_operator_and_operands() {
        NamedColumn l = new NamedColumn("id", null, "p");
        NamedColumn r = new NamedColumn("id", null, "d");
        JoinFilter jf = new JoinFilter(l, JoinFilter.Operator.Gte, r);

        assertSame(l, jf.left());
        assertSame(r, jf.right());
        assertEquals(JoinFilter.Operator.Gte, jf.operator());
    }
}