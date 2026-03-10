package io.sqm.parser.mysql;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlDeleteStatementParserTest {

    @Test
    void parsesDeleteUsingJoinStatement() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(DeleteStatement.class,
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id WHERE orders.state = 'closed'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().table().name().value());
        assertEquals(1, result.value().using().size());
        assertEquals(1, result.value().joins().size());
    }

    @Test
    void statementEntryPointParsesMysqlDeleteUsingJoin() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Statement.class,
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(DeleteStatement.class, result.value());
    }

    @Test
    void rejectsDeleteJoinWithoutUsing() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(DeleteStatement.class,
            "DELETE FROM users INNER JOIN orders ON users.id = orders.user_id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("requires USING"));
    }

    @Test
    void parserRejectsDeleteUsingJoinWhenCapabilityIsMissing() {
        var ctx = ParseContext.of(new MySqlSpecs(SqlDialectVersion.of(5, 7)));
        var result = ctx.parse(DeleteStatement.class,
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... USING"));
    }
}
