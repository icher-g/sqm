package io.sqm.parser.ansi;

import io.sqm.core.LockMode;
import io.sqm.core.LockingClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LockingClauseParser Tests")
class LockingClauseParserTest {

    private ParseContext ctx;
    private final LockingClauseParser parser = new LockingClauseParser();
    private final IdentifierQuoting quoting = IdentifierQuoting.of('"');

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
    }

    @Test
    @DisplayName("Parse simple FOR UPDATE")
    void parseSimpleForUpdate() {
        var result = parser.parse(Cursor.of("FOR UPDATE", quoting), ctx);

        assertTrue(result.ok());
        var clause = result.value();
        assertNotNull(clause);
        assertEquals(LockMode.UPDATE, clause.mode());
        assertTrue(clause.ofTables().isEmpty());
        assertFalse(clause.nowait());
        assertFalse(clause.skipLocked());
    }

    @Test
    @DisplayName("Parse FOR UPDATE case insensitive")
    void parseForUpdateCaseInsensitive() {
        var result1 = ctx.parse(LockingClause.class, "for update");
        var result2 = ctx.parse(LockingClause.class, "FOR UPDATE");
        var result3 = ctx.parse(LockingClause.class, "For Update");

        assertTrue(result1.ok());
        assertTrue(result2.ok());
        assertTrue(result3.ok());
    }

    @Test
    @DisplayName("Parse FOR UPDATE OF throws unsupported exception")
    void parseForUpdateOfThrows() {
        var result = parser.parse(Cursor.of("FOR UPDATE OF users", quoting), ctx);
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("Parse FOR UPDATE NOWAIT throws unsupported exception")
    void parseForUpdateNowaitThrows() {
        var result = parser.parse(Cursor.of("FOR UPDATE NOWAIT", quoting), ctx);
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("Parse FOR UPDATE SKIP LOCKED throws unsupported exception")
    void parseForUpdateSkipLockedThrows() {
        var result = parser.parse(Cursor.of("FOR UPDATE SKIP LOCKED", quoting), ctx);
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("Parse FOR without UPDATE fails")
    void parseForWithoutUpdate() {
        var result = ctx.parse(LockingClause.class, "FOR");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse UPDATE without FOR fails")
    void parseUpdateWithoutFor() {
        var result = ctx.parse(LockingClause.class, "UPDATE");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse empty string fails")
    void parseEmptyString() {
        var result = ctx.parse(LockingClause.class, "");

        assertFalse(result.ok());
    }

    @Test
    @DisplayName("Parse with extra whitespace")
    void parseWithExtraWhitespace() {
        var result = parser.parse(Cursor.of("FOR    UPDATE", quoting), ctx);

        assertTrue(result.ok());
        assertEquals(LockMode.UPDATE, result.value().mode());
    }

    @Test
    @DisplayName("Parse FOR SHARE is not supported in ANSI")
    void parseForShareNotSupported() {
        var result = ctx.parse(LockingClause.class, "FOR SHARE");

        assertFalse(result.ok());
    }

    @Test
    @DisplayName("Parse FOR KEY SHARE is not supported in ANSI")
    void parseForKeyShareNotSupported() {
        var result = parser.parse(Cursor.of("FOR KEY SHARE", quoting), ctx);

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("FOR KEY SHARE is not supported"));
    }

    @Test
    @DisplayName("Parse FOR NO KEY UPDATE is not supported in ANSI")
    void parseForNoKeyUpdateNotSupported() {
        var result = parser.parse(Cursor.of("FOR NO KEY UPDATE", quoting), ctx);

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("FOR NO KEY UPDATE is not supported"));
    }

    @Test
    @DisplayName("Parse FOR with invalid mode fails")
    void parseForWithInvalidModeFails() {
        var result = parser.parse(Cursor.of("FOR INVALID", quoting), ctx);

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse FOR KEY without SHARE fails")
    void parseForKeyWithoutShareFails() {
        var result = parser.parse(Cursor.of("FOR KEY", quoting), ctx);

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse FOR NO without KEY UPDATE fails")
    void parseForNoWithoutKeyUpdateFails() {
        var result = parser.parse(Cursor.of("FOR NO", quoting), ctx);

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse FOR NO KEY without UPDATE fails")
    void parseForNoKeyWithoutUpdateFails() {
        var result = parser.parse(Cursor.of("FOR NO KEY", quoting), ctx);

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Parse SKIP without LOCKED fails")
    void parseSkipWithoutLockedFails() {
        var result = parser.parse(Cursor.of("FOR UPDATE SKIP", quoting), ctx);

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Target type is LockingClause")
    void targetTypeIsLockingClause() {
        assertEquals(io.sqm.core.LockingClause.class, parser.targetType());
    }
}
