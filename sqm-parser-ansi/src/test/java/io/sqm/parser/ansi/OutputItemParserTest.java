package io.sqm.parser.ansi;

import io.sqm.core.OutputItem;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputItemParserTest {

    @Test
    void parsesOutputItemWithoutAlias() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(OutputItem.class, "1");

        assertTrue(result.ok(), result.errorMessage());
        assertNull(result.value().alias());
    }

    @Test
    void parsesOutputItemWithAlias() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(OutputItem.class, "1 AS out_id");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("out_id", result.value().alias().value());
    }

    @Test
    void errorWhenExpressionMissing() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(OutputItem.class, "AS out_id");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }
}
