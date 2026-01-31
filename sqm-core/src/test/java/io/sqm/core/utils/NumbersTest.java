package io.sqm.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumbersTest {

    @Test
    void detectsPositiveIntegers() {
        assertTrue(Numbers.isPositiveInteger("1"));
        assertTrue(Numbers.isPositiveInteger("123"));
        assertTrue(Numbers.isPositiveInteger("001"));
    }

    @Test
    void rejectsNonPositiveIntegers() {
        assertFalse(Numbers.isPositiveInteger(""));
        assertFalse(Numbers.isPositiveInteger("-1"));
        assertFalse(Numbers.isPositiveInteger("1.0"));
        assertFalse(Numbers.isPositiveInteger("12a"));
    }
}
