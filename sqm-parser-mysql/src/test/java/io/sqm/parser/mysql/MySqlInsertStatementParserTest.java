package io.sqm.parser.mysql;

import io.sqm.core.InsertStatement;
import io.sqm.core.RowExpr;
import io.sqm.core.Statement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
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
    void parsesPlainInsertValuesStatement() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1)");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(InsertStatement.InsertMode.STANDARD, result.value().insertMode());
        assertEquals(InsertStatement.OnConflictAction.NONE, result.value().onConflictAction());
        assertInstanceOf(RowExpr.class, result.value().source());
    }

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
    void parsesOnDuplicateKeyUpdateWithQualifiedTarget() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users (id, name) VALUES (1, 'alice') ON DUPLICATE KEY UPDATE users.name = 'alice2'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(java.util.List.of("users", "name"), result.value().conflictUpdateAssignments().getFirst().column().values());
    }

    @Test
    void parsesInsertReturningWhenCapabilityIsEnabled() {
        var ctx = ParseContext.of(new ReturningMySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1) RETURNING id");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().returning().size());
    }

    @Test
    void rejectsInsertReturningInMysql80() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1) RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("INSERT ... RETURNING"));
    }

    @Test
    void rejectsInsertReturningInMysql57() {
        var ctx = ParseContext.of(new MySqlSpecs(SqlDialectVersion.of(5, 7)));
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1) RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("INSERT ... RETURNING"));
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
    void rejectsOnClauseWithoutDuplicateKeyword() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1) ON KEY UPDATE id = 2");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected DUPLICATE after ON"));
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

    @Test
    void parserRejectsOnDuplicateWhenCapabilityIsMissing() {
        var parser = new MySqlInsertStatementParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("INSERT INTO users VALUES (1) ON DUPLICATE KEY UPDATE id = 2", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
        assertTrue(result.errorMessage() != null && !Objects.requireNonNull(result.errorMessage()).isBlank());
    }

    private static final class ReturningMySqlSpecs extends MySqlSpecs {
        @Override
        public DialectCapabilities capabilities() {
            var delegate = super.capabilities();
            return feature -> feature == SqlFeature.DML_RETURNING || delegate.supports(feature);
        }
    }
}