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
    void parsesJoinedUpdateWithAliasAndIndexHints() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE users USE INDEX (idx_users_name) AS u INNER JOIN orders FORCE INDEX FOR JOIN (idx_orders_user) AS o ON u.id = o.user_id SET name = 'alice' WHERE o.state = 'closed'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("u", result.value().table().alias().value());
        assertEquals(1, result.value().table().indexHints().size());
        assertEquals(1, result.value().joins().size());
    }

    @Test
    void parsesStandardUpdateWithoutJoins() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertTrue(result.value().joins().isEmpty());
    }

    @Test
    void rejectsUpdateReturningInMysql80() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice' RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("UPDATE ... RETURNING"));
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
    void rejectsInvalidJoinedUpdate() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users INNER JOIN orders SET name = 'alice'");

        assertTrue(result.isError());
    }

    @Test
    void parserRejectsJoinedUpdateWhenCapabilityIsMissing() {
        var ctx = ParseContext.of(new MySqlSpecs(SqlDialectVersion.of(5, 7)));
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice'");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("UPDATE ... JOIN"));
    }

    @Test
    void rejectsUpdateReturningInMysql57() {
        var ctx = ParseContext.of(new MySqlSpecs(SqlDialectVersion.of(5, 7)));
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice' RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("UPDATE ... RETURNING"));
    }
}


