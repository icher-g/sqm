package io.sqm.parser.mysql;

import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlUpdateStatementParserTest {

    @Test
    void parsesJoinedUpdateStatement() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice' WHERE orders.state = 'closed'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().table().name().value());
        assertEquals(1, result.value().joins().size());
        assertEquals(1, result.value().assignments().size());
    }

    @Test
    void statementEntryPointParsesMysqlJoinedUpdate() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Statement.class,
            "UPDATE users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(UpdateStatement.class, result.value());
    }

    @Test
    void parserRejectsJoinedUpdateWhenCapabilityIsMissing() {
        var ctx = ParseContext.of(new MySqlSpecs(SqlDialectVersion.of(5, 7)));
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice'");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("UPDATE ... JOIN"));
    }
}
