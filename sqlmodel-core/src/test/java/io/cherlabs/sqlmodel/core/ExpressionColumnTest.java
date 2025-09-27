package io.cherlabs.sqlmodel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionColumnTest {

    @Test
    void as_sets_alias() {
        ExpressionColumn c = new ExpressionColumn("lower(name)", null);
        ExpressionColumn a = c.as("lname");
        assertEquals("lower(name)", a.expression());
        assertEquals("lname", a.alias());
        assertNotSame(c, a);
    }
}