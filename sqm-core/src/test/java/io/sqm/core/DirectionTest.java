package io.sqm.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Direction enum")
class DirectionTest {

    @Test
    @DisplayName("ASC value exists and is accessible")
    void asc() {
        assertEquals(Direction.ASC, Direction.ASC);
        assertNotNull(Direction.ASC);
    }

    @Test
    @DisplayName("DESC value exists and is accessible")
    void desc() {
        assertEquals(Direction.DESC, Direction.DESC);
        assertNotNull(Direction.DESC);
    }

    @Test
    @DisplayName("All enum values are distinct")
    void valuesDistinct() {
        Direction[] values = Direction.values();
        assertEquals(2, values.length);
        assertNotEquals(Direction.ASC, Direction.DESC);
    }

    @Test
    @DisplayName("valueOf() works for valid strings")
    void valueOfValid() {
        assertEquals(Direction.ASC, Direction.valueOf("ASC"));
        assertEquals(Direction.DESC, Direction.valueOf("DESC"));
    }

    @Test
    @DisplayName("valueOf() throws for invalid string")
    void valueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Direction.valueOf("INVALID"));
    }

    @Test
    @DisplayName("Enum constants can be used in switch statements")
    void switchUsage() {
        Direction dir = Direction.ASC;
        String result = switch (dir) {
            case ASC -> "ascending";
            case DESC -> "descending";
        };
        assertEquals("ascending", result);

        dir = Direction.DESC;
        result = switch (dir) {
            case ASC -> "ascending";
            case DESC -> "descending";
        };
        assertEquals("descending", result);
    }
}
