package io.sqm.parser.ansi;

import io.sqm.core.OutputInto;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputIntoParserTest {

    @Test
    void parsesIntoTargetWithoutColumns() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(OutputInto.class, "INTO my_table");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("my_table", result.value().target().name().value());
        assertTrue(result.value().columns().isEmpty());
    }

    @Test
    void parsesIntoTargetWithColumns() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(OutputInto.class, "INTO my_table (col_a, col_b)");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("my_table", result.value().target().name().value());
        assertEquals(2, result.value().columns().size());
    }

    @Test
    void errorWhenIntoTargetMissing() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(OutputInto.class, "INTO");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }

    @Test
    void errorWhenOutputIntoColumnsUnterminated() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(OutputInto.class, "INTO my_table (col_a, col_b");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }
}
