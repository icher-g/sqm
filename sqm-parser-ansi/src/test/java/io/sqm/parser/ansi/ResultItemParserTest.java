package io.sqm.parser.ansi;

import io.sqm.core.ResultItem;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultItemParserTest {

    @Test
    void parsesOutputItemWithoutAlias() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(ResultItem.class, "1");

        assertTrue(result.ok(), result.errorMessage());
        assertNull(result.value().matchResultItem().expr(e -> e.alias()).orElse(null));
    }

    @Test
    void parsesOutputItemWithAlias() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(ResultItem.class, "1 AS out_id");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("out_id", result.value().matchResultItem().expr(e -> e.alias().value()).orElse(null));
    }

    @Test
    void parsesStarAndQualifiedStarItems() {
        var ctx = ParseContext.of(new TestSpecs());

        var star = ctx.parse(ResultItem.class, "*");
        var qualifiedStar = ctx.parse(ResultItem.class, "u.*");

        assertTrue(star.ok(), star.errorMessage());
        assertEquals("star", star.value().matchResultItem().star(ignore -> "star").orElse(null));
        assertTrue(qualifiedStar.ok(), qualifiedStar.errorMessage());
        assertEquals("u", qualifiedStar.value().matchResultItem().qualifiedStar(item -> item.qualifier().value()).orElse(null));
    }

    @Test
    void errorWhenExpressionMissing() {
        var ctx = ParseContext.of(new TestSpecs());
        var result = ctx.parse(ResultItem.class, "AS out_id");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }
}
