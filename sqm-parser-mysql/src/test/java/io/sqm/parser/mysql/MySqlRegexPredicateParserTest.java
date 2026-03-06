package io.sqm.parser.mysql;

import io.sqm.core.Predicate;
import io.sqm.core.RegexMode;
import io.sqm.core.RegexPredicate;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlRegexPredicateParserTest {

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
}


