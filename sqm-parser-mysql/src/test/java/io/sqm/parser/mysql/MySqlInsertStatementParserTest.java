package io.sqm.parser.mysql;

import io.sqm.core.InsertStatement;
import io.sqm.core.RowExpr;
import io.sqm.core.Statement;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlInsertStatementParserTest {

    @Test
    void parsesInsertIgnoreValuesStatement() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT IGNORE INTO users (id) VALUES (1)");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(InsertStatement.InsertMode.IGNORE, result.value().insertMode());
        assertInstanceOf(RowExpr.class, result.value().source());
    }

    @Test
    void parsesReplaceIntoValuesStatement() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "REPLACE INTO users (id, name) VALUES (1, 'alice')");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(InsertStatement.InsertMode.REPLACE, result.value().insertMode());
        assertInstanceOf(RowExpr.class, result.value().source());
    }

    @Test
    void parsesOnDuplicateKeyUpdateClause() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users (id, name) VALUES (1, 'alice') ON DUPLICATE KEY UPDATE name = 'alice2'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(InsertStatement.OnConflictAction.DO_UPDATE, result.value().onConflictAction());
        assertTrue(result.value().conflictTarget().isEmpty());
        assertEquals(1, result.value().conflictUpdateAssignments().size());
    }

    @Test
    void statementEntryPointParsesMysqlInsert() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Statement.class, "INSERT IGNORE INTO users VALUES (1)");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(InsertStatement.class, result.value());
    }

    @Test
    void statementEntryPointParsesReplaceInto() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Statement.class, "REPLACE INTO users VALUES (1)");

        assertTrue(result.ok(), result.errorMessage());
        var statement = assertInstanceOf(InsertStatement.class, result.value());
        assertEquals(InsertStatement.InsertMode.REPLACE, statement.insertMode());
    }

    @Test
    void rejectsOnConflictSyntaxInMysqlDialect() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1) ON CONFLICT (id) DO NOTHING");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("INSERT ... ON CONFLICT"));
    }

    @Test
    void parserRejectsInsertIgnoreWhenCapabilityIsMissing() {
        var parser = new MySqlInsertStatementParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("INSERT IGNORE INTO users VALUES (1)", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("INSERT IGNORE"));
    }

    @Test
    void parserRejectsReplaceIntoWhenCapabilityIsMissing() {
        var parser = new MySqlInsertStatementParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("REPLACE INTO users VALUES (1)", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("REPLACE INTO"));
    }
}
