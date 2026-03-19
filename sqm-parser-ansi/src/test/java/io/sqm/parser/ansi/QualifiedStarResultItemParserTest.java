package io.sqm.parser.ansi;

import io.sqm.core.QualifiedStarResultItem;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualifiedStarResultItemParserTest {

    @Test
    void parsesQualifiedStarResultItem() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(QualifiedStarResultItem.class, "u.*");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("u", result.value().qualifier().value());
    }

    @Test
    void errorsWhenQualifiedStarIsIncomplete() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(QualifiedStarResultItem.class, "u.");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }
}
