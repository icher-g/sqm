package io.sqm.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DistinctSpec interface")
class DistinctSpecTest {

    @Test
    @DisplayName("TRUE constant is not null")
    void trueConstant() {
        assertNotNull(DistinctSpec.TRUE);
    }

    @Test
    @DisplayName("FALSE constant is null")
    void falseConstant() {
        assertNull(DistinctSpec.FALSE);
    }

    @Test
    @DisplayName("TRUE is instance of DistinctSpec")
    void trueIsDistinctSpec() {
        assertInstanceOf(DistinctSpec.class, DistinctSpec.TRUE);
    }

    @Test
    @DisplayName("TRUE and FALSE are distinct")
    void trueAndFalseDistinct() {
        assertNotEquals(DistinctSpec.TRUE, DistinctSpec.FALSE);
    }

    @Test
    @DisplayName("TRUE constant is a Node")
    void trueIsNode() {
        assertInstanceOf(Node.class, DistinctSpec.TRUE);
    }

    @Test
    @DisplayName("TRUE constant can be used in boolean context")
    void useInBooleanContext() {
        DistinctSpec distinct = DistinctSpec.TRUE;
        assertNotNull(distinct);

        distinct = DistinctSpec.FALSE;
        assertNull(distinct);
    }

    @Test
    @DisplayName("Multiple references to TRUE return same object")
    void trueSameObject() {
        DistinctSpec first = DistinctSpec.TRUE;
        DistinctSpec second = DistinctSpec.TRUE;
        assertSame(first, second);
    }

    @Test
    @DisplayName("Equality check works for TRUE constant")
    void trueEquality() {
        DistinctSpec distinct1 = DistinctSpec.TRUE;
        DistinctSpec distinct2 = DistinctSpec.TRUE;
        assertEquals(distinct1, distinct2);
    }
}
