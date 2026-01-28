package io.sqm.parser.spi;

import io.sqm.core.ColumnExpr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchResultTest {

    @Test
    void shouldCreateMatchedResultWithSuccess() {
        var column = ColumnExpr.of("test");
        var parseResult = ParseResult.ok(column);
        var matchResult = MatchResult.matched(parseResult);
        
        assertTrue(matchResult.match());
        assertSame(parseResult, matchResult.result());
    }

    @Test
    void shouldCreateMatchedResultWithError() {
        var parseResult = ParseResult.<ColumnExpr>error("test error", 10);
        var matchResult = MatchResult.matched(parseResult);
        
        assertTrue(matchResult.match());
        assertSame(parseResult, matchResult.result());
    }

    @Test
    void shouldCreateNotMatchedResult() {
        var matchResult = MatchResult.<ColumnExpr>notMatched();
        
        assertFalse(matchResult.match());
        assertNull(matchResult.result());
    }

    @Test
    void shouldSupportRecordEquality() {
        var parseResult = ParseResult.ok(ColumnExpr.of("test"));
        var match1 = MatchResult.matched(parseResult);
        var match2 = MatchResult.matched(parseResult);
        var notMatch1 = MatchResult.<ColumnExpr>notMatched();
        var notMatch2 = MatchResult.<ColumnExpr>notMatched();
        
        assertEquals(match1, match2);
        assertEquals(notMatch1, notMatch2);
        assertNotEquals(match1, notMatch1);
    }

    @Test
    void shouldSupportRecordHashCode() {
        var parseResult = ParseResult.ok(ColumnExpr.of("test"));
        var match1 = MatchResult.matched(parseResult);
        var match2 = MatchResult.matched(parseResult);
        
        assertEquals(match1.hashCode(), match2.hashCode());
    }

    @Test
    void shouldSupportRecordToString() {
        var parseResult = ParseResult.ok(ColumnExpr.of("test"));
        var matchResult = MatchResult.matched(parseResult);
        
        String str = matchResult.toString();
        assertTrue(str.contains("true") || str.contains("match"));
    }
}
