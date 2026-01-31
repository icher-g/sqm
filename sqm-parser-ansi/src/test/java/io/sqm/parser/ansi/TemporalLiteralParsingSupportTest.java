package io.sqm.parser.ansi;

import io.sqm.core.TimeZoneSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemporalLiteralParsingSupportTest {

    private final AnsiSpecs specs = new AnsiSpecs();

    @Test
    void skipTimeZoneSpec_returnsCurrentOffset_whenNoClause() {
        var cur = Cursor.of("TIME '10:11:12'", specs.identifierQuoting());
        assertEquals(1, TemporalLiteralParsingSupport.skipTimeZoneSpec(cur, 1));
    }

    @Test
    void skipTimeZoneSpec_returnsIndex_whenClausePresent() {
        var cur = Cursor.of("TIME WITH TIME ZONE '10:11:12'", specs.identifierQuoting());
        assertEquals(4, TemporalLiteralParsingSupport.skipTimeZoneSpec(cur, 1));

        var cur2 = Cursor.of("TIME WITHOUT TIME ZONE '10:11:12'", specs.identifierQuoting());
        assertEquals(4, TemporalLiteralParsingSupport.skipTimeZoneSpec(cur2, 1));
    }

    @Test
    void skipTimeZoneSpec_returnsMinusOne_forInvalidClause() {
        var cur = Cursor.of("TIME WITH ZONE '10:11:12'", specs.identifierQuoting());
        assertEquals(-1, TemporalLiteralParsingSupport.skipTimeZoneSpec(cur, 1));
    }

    @Test
    void parseTimeZoneSpec_recognizes_with_and_without_time_zone() {
        var with = Cursor.of("WITH TIME ZONE '10:11:12'", specs.identifierQuoting());
        assertEquals(TimeZoneSpec.WITH_TIME_ZONE, TemporalLiteralParsingSupport.parseTimeZoneSpec(with));
        assertTrue(with.match(TokenType.STRING));

        var without = Cursor.of("WITHOUT TIME ZONE '10:11:12'", specs.identifierQuoting());
        assertEquals(TimeZoneSpec.WITHOUT_TIME_ZONE, TemporalLiteralParsingSupport.parseTimeZoneSpec(without));
        assertTrue(without.match(TokenType.STRING));
    }

    @Test
    void parseTimeZoneSpec_returnsNone_whenMissing() {
        var cur = Cursor.of("'10:11:12'", specs.identifierQuoting());
        assertEquals(TimeZoneSpec.NONE, TemporalLiteralParsingSupport.parseTimeZoneSpec(cur));
        assertTrue(cur.match(TokenType.STRING));
    }

    @Test
    void parseTimeZoneSpec_throws_on_invalid_sequence() {
        var cur = Cursor.of("WITH ZONE '10:11:12'", specs.identifierQuoting());
        assertThrows(ParserException.class, () -> TemporalLiteralParsingSupport.parseTimeZoneSpec(cur));
    }
}
