package io.sqm.parser.sqlserver;

import io.sqm.core.Table;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableParserTest {

    @Test
    void parsesAliasWithAsAndLockHints() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(Table.class, "users AS u WITH (UPDLOCK, HOLDLOCK)");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("u", result.value().alias().value());
        assertEquals(2, result.value().hints().size());
        assertEquals("UPDLOCK", result.value().hints().getFirst().name().value());
    }

    @Test
    void rejectsDuplicateLockHints() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(Table.class, "users WITH (HOLDLOCK, HOLDLOCK)");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Duplicate"));
    }

    @Test
    void rejectsMissingHintAfterComma() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(Table.class, "users WITH (UPDLOCK, )");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected SQL Server table hint"));
    }

    @Test
    void parserRejectsLockHintsWhenCapabilityIsMissing() {
        var parser = new TableParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("users WITH (NOLOCK)", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("table hints"));
    }
}
