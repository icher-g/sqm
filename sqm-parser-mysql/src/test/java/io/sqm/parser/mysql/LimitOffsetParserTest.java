package io.sqm.parser.mysql;

import io.sqm.core.LimitOffset;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitOffsetParserTest {

    private final ParseContext ctx = ParseContext.of(new MySqlSpecs());
    private final LimitOffsetParser parser = new LimitOffsetParser();

    @Test
    void parsesLimitOnly() {
        var result = ctx.parse(parser, "LIMIT 10");

        assertTrue(result.ok());
        assertEquals(LimitOffset.of(10L, null), result.value());
    }

    @Test
    void parsesLimitWithOffset() {
        var result = ctx.parse(parser, "LIMIT 10 OFFSET 5");

        assertTrue(result.ok());
        assertEquals(LimitOffset.of(10L, 5L), result.value());
    }

    @Test
    void parsesLimitCommaForm() {
        var result = ctx.parse(parser, "LIMIT 5, 10");

        assertTrue(result.ok());
        assertEquals(LimitOffset.of(10L, 5L), result.value());
    }

    @Test
    void errorsOnOffsetWithoutLimit() {
        var result = ctx.parse(parser, "OFFSET 5");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("OFFSET without LIMIT"));
    }

    @Test
    void errorsOnLimitAll() {
        var result = ctx.parse(parser, "LIMIT ALL");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("LIMIT ALL"));
    }

    @Test
    void errorsOnFetchSyntax() {
        var result = ctx.parse(parser, "FETCH FIRST 5 ROWS ONLY");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("FETCH"));
    }

    @Test
    void errorsOnFetchAfterLimitExpression() {
        var result = ctx.parse(parser, "LIMIT 10 FETCH FIRST 5 ROWS ONLY");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("FETCH"));
    }

    @Test
    void errorsOnCommaFormCombinedWithOffset() {
        var result = ctx.parse(parser, "LIMIT 5, 10 OFFSET 1");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("OFFSET/FETCH"));
    }

    @Test
    void errorsOnCommaFormCombinedWithFetch() {
        var result = ctx.parse(parser, "LIMIT 5, 10 FETCH FIRST 1 ROWS ONLY");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("OFFSET/FETCH"));
    }

    @Test
    void errorsWhenLimitExpressionInvalid() {
        var result = ctx.parse(parser, "LIMIT");

        assertTrue(result.isError());
    }

    @Test
    void errorsWhenOffsetExpressionInvalid() {
        var result = ctx.parse(parser, "LIMIT 10 OFFSET");

        assertTrue(result.isError());
    }
}


