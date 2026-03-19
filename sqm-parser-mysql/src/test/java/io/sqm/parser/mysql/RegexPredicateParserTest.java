package io.sqm.parser.mysql;

import io.sqm.core.Predicate;
import io.sqm.core.RegexMode;
import io.sqm.core.RegexPredicate;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexPredicateParserTest {

    @Test
    void parsesRegexpPredicate() {
        var result = ParseContext.of(new MySqlSpecs()).parse(Predicate.class, "name REGEXP '^a'");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(RegexPredicate.class, result.value());
        assertEquals(RegexMode.MATCH, predicate.mode());
        assertFalse(predicate.negated());
    }

    @Test
    void parsesRlikePredicate() {
        var result = ParseContext.of(new MySqlSpecs()).parse(Predicate.class, "name RLIKE '^a'");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(RegexPredicate.class, result.value());
        assertEquals(RegexMode.MATCH, predicate.mode());
        assertFalse(predicate.negated());
    }

    @Test
    void parsesNotRegexpPredicate() {
        var result = ParseContext.of(new MySqlSpecs()).parse(Predicate.class, "name NOT REGEXP '^a'");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(RegexPredicate.class, result.value());
        assertTrue(predicate.negated());
    }

    @Test
    void parsesNotRlikePredicate() {
        var result = ParseContext.of(new MySqlSpecs()).parse(Predicate.class, "name NOT RLIKE '^a'");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(RegexPredicate.class, result.value());
        assertTrue(predicate.negated());
    }

    @Test
    void ansiDialectDoesNotAcceptMysqlRegexpKeywordForms() {
        var result = ParseContext.of(new AnsiSpecs()).parse(Predicate.class, "name REGEXP '^a'");

        assertTrue(result.isError());
    }

    @Test
    void parserTargetTypeIsRegexPredicate() {
        var parser = new RegexPredicateParser();
        assertEquals(RegexPredicate.class, parser.targetType());
    }

    @Test
    void parseInfixFailsWhenRegexFeatureIsUnsupported() {
        var parser = new RegexPredicateParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("REGEXP '^a'", ctx.identifierQuoting());

        var result = parser.parse(io.sqm.dsl.Dsl.col("name"), cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("not supported by this dialect"));
    }

    @Test
    void parseInfixFailsWhenKeywordIsNotRegexpOrRlike() {
        var parser = new RegexPredicateParser();
        var ctx = ParseContext.of(new MySqlSpecs());
        var cur = Cursor.of("NOT LIKE '^a'", ctx.identifierQuoting());

        var error = org.junit.jupiter.api.Assertions.assertThrows(io.sqm.parser.core.ParserException.class,
            () -> parser.parse(io.sqm.dsl.Dsl.col("name"), cur, ctx));

        assertTrue(error.getMessage().contains("Expected REGEXP or RLIKE"));
    }

    @Test
    void parseInfixFailsWhenPatternIsMissing() {
        var parser = new RegexPredicateParser();
        var ctx = ParseContext.of(new MySqlSpecs());
        var cur = Cursor.of("REGEXP", ctx.identifierQuoting());

        var result = parser.parse(io.sqm.dsl.Dsl.col("name"), cur, ctx);

        assertTrue(result.isError());
    }

    @Test
    void parseEntryFailsWhenLeftExpressionCannotBeParsed() {
        var parser = new RegexPredicateParser();
        var ctx = ParseContext.of(new MySqlSpecs());
        var cur = Cursor.of(")", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
    }
}


