package io.sqm.parser.mysql;

import io.sqm.core.LockMode;
import io.sqm.core.LockingClause;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlLockingClauseParserTest {

    private final ParseContext ctx = ParseContext.of(new MySqlSpecs());

    @Test
    void parsesForUpdate() {
        var result = ctx.parse(LockingClause.class, "FOR UPDATE");

        assertTrue(result.ok());
        assertEquals(LockMode.UPDATE, result.value().mode());
        assertFalse(result.value().nowait());
        assertFalse(result.value().skipLocked());
    }

    @Test
    void parsesForShare() {
        var result = ctx.parse(LockingClause.class, "FOR SHARE");

        assertTrue(result.ok());
        assertEquals(LockMode.SHARE, result.value().mode());
    }

    @Test
    void parsesForUpdateNowait() {
        var result = ctx.parse(LockingClause.class, "FOR UPDATE NOWAIT");

        assertTrue(result.ok());
        assertEquals(LockMode.UPDATE, result.value().mode());
        assertTrue(result.value().nowait());
        assertFalse(result.value().skipLocked());
    }

    @Test
    void parsesForUpdateSkipLocked() {
        var result = ctx.parse(LockingClause.class, "FOR UPDATE SKIP LOCKED");

        assertTrue(result.ok());
        assertEquals(LockMode.UPDATE, result.value().mode());
        assertFalse(result.value().nowait());
        assertTrue(result.value().skipLocked());
    }

    @Test
    void rejectsForKeyShare_withDeterministicMessage() {
        var result = ctx.parse(LockingClause.class, "FOR KEY SHARE");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("FOR KEY SHARE is not supported by this dialect"));
    }

    @Test
    void rejectsForNoKeyUpdate_withDeterministicMessage() {
        var result = ctx.parse(LockingClause.class, "FOR NO KEY UPDATE");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("FOR NO KEY UPDATE is not supported by this dialect"));
    }
}
