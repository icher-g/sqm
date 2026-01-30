package io.sqm.render.defaults;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultOperatorsTest {

    @Test
    void returnsExpectedOperatorTokens() {
        var ops = new DefaultOperators();

        assertEquals("IN", ops.in());
        assertEquals("NOT IN", ops.notIn());
        assertEquals("BETWEEN", ops.between());
        assertEquals("=", ops.eq());
        assertEquals("<>", ops.ne());
        assertEquals("<", ops.lt());
        assertEquals("<=", ops.lte());
        assertEquals(">", ops.gt());
        assertEquals(">=", ops.gte());
        assertEquals("LIKE", ops.like());
        assertEquals("NOT LIKE", ops.notLike());
        assertEquals("ILIKE", ops.ilike());
        assertEquals("NOT ILIKE", ops.notIlike());
        assertEquals("SIMILAR TO", ops.similarTo());
        assertEquals("NOT SIMILAR TO", ops.notSimilarTo());
        assertEquals("IS NULL", ops.isNull());
        assertEquals("IS NOT NULL", ops.isNotNull());
        assertEquals("IS DISTINCT FROM", ops.isDistinctFrom());
        assertEquals("IS NOT DISTINCT FROM", ops.isNotDistinctFrom());
        assertEquals("AND", ops.and());
        assertEquals("OR", ops.or());
        assertEquals("NOT", ops.not());
        assertEquals("+", ops.add());
        assertEquals("-", ops.sub());
        assertEquals("*", ops.mul());
        assertEquals("/", ops.div());
        assertEquals("-", ops.neg());
    }
}
