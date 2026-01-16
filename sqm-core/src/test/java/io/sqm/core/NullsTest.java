package io.sqm.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Nulls enum")
class NullsTest {

    @Test
    @DisplayName("DEFAULT value exists and is accessible")
    void defaultValue() {
        assertEquals(Nulls.DEFAULT, Nulls.DEFAULT);
        assertNotNull(Nulls.DEFAULT);
    }

    @Test
    @DisplayName("FIRST value exists and is accessible")
    void first() {
        assertEquals(Nulls.FIRST, Nulls.FIRST);
        assertNotNull(Nulls.FIRST);
    }

    @Test
    @DisplayName("LAST value exists and is accessible")
    void last() {
        assertEquals(Nulls.LAST, Nulls.LAST);
        assertNotNull(Nulls.LAST);
    }

    @Test
    @DisplayName("All enum values are distinct")
    void valuesDistinct() {
        Nulls[] values = Nulls.values();
        assertEquals(3, values.length);
        assertNotEquals(Nulls.DEFAULT, Nulls.FIRST);
        assertNotEquals(Nulls.FIRST, Nulls.LAST);
        assertNotEquals(Nulls.DEFAULT, Nulls.LAST);
    }

    @Test
    @DisplayName("valueOf() works for valid strings")
    void valueOfValid() {
        assertEquals(Nulls.DEFAULT, Nulls.valueOf("DEFAULT"));
        assertEquals(Nulls.FIRST, Nulls.valueOf("FIRST"));
        assertEquals(Nulls.LAST, Nulls.valueOf("LAST"));
    }

    @Test
    @DisplayName("valueOf() throws for invalid string")
    void valueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Nulls.valueOf("INVALID"));
    }

    @Test
    @DisplayName("Enum constants can be used in switch statements")
    void switchUsage() {
        Nulls nulls = Nulls.DEFAULT;
        String result = switch (nulls) {
            case DEFAULT -> "default";
            case FIRST -> "first";
            case LAST -> "last";
        };
        assertEquals("default", result);

        nulls = Nulls.FIRST;
        result = switch (nulls) {
            case DEFAULT -> "default";
            case FIRST -> "first";
            case LAST -> "last";
        };
        assertEquals("first", result);

        nulls = Nulls.LAST;
        result = switch (nulls) {
            case DEFAULT -> "default";
            case FIRST -> "first";
            case LAST -> "last";
        };
        assertEquals("last", result);
    }

    @Test
    @DisplayName("ordinal() returns correct position")
    void ordinal() {
        assertEquals(0, Nulls.DEFAULT.ordinal());
        assertEquals(1, Nulls.FIRST.ordinal());
        assertEquals(2, Nulls.LAST.ordinal());
    }
}
