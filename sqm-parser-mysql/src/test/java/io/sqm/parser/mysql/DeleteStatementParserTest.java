package io.sqm.parser.mysql;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Statement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteStatementParserTest {

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
    void parsesDeleteUsingJoinWithAliasAndIndexHints() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(DeleteStatement.class,
            "DELETE FROM users USE INDEX (idx_users_name) AS u USING users USE INDEX (idx_users_name) AS u INNER JOIN orders FORCE INDEX FOR JOIN (idx_orders_user) AS o ON u.id = o.user_id WHERE o.state = 'closed'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("u", result.value().table().alias().value());
        assertEquals(1, result.value().table().indexHints().size());
        assertEquals("u", result.value().using().getFirst().matchTableRef().table(t -> t.alias().value()).orElse(null));
        assertEquals(1, result.value().joins().size());
    }

    @Test
    void parsesOptimizerHintAfterDeleteKeyword() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(DeleteStatement.class,
            "DELETE /*+ BKA(users) */ FROM users WHERE users.id = 1");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(java.util.List.of("BKA(users)"), result.value().optimizerHints());
    }

    @Test
    void parsesDeleteUsingWithoutJoins() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users USING users WHERE users.id = 1");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().using().size());
        assertTrue(result.value().joins().isEmpty());
    }

    @Test
    void rejectsDeleteReturningInMysql80() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users WHERE users.id = 1 RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... RETURNING"));
    }

    @Test
    void parsesDeleteReturningWhenCapabilityIsEnabled() {
        var ctx = ParseContext.of(new ReturningMySqlSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users WHERE users.id = 1 RETURNING id");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().result().items().size());
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
    void rejectsDeleteUsingWithoutSource() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users USING");

        assertTrue(result.isError());
    }

    @Test
    void parserRejectsDeleteUsingJoinWhenCapabilityIsMissing() {
        var ctx = ParseContext.of(new MySqlSpecs(SqlDialectVersion.of(5, 7)));
        var result = ctx.parse(DeleteStatement.class,
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... USING"));
    }

    @Test
    void parserRejectsDeleteOptimizerHintWhenCapabilityIsMissing() {
        var ctx = ParseContext.of(new NoOptimizerHintMySqlSpecs());
        var result = ctx.parse(DeleteStatement.class,
            "DELETE /*+ BKA(users) */ FROM users WHERE users.id = 1");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Optimizer hint comments"));
    }

    @Test
    void rejectsDeleteReturningInMysql57() {
        var ctx = ParseContext.of(new MySqlSpecs(SqlDialectVersion.of(5, 7)));
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users WHERE users.id = 1 RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... RETURNING"));
    }

    private static final class ReturningMySqlSpecs extends MySqlSpecs {
        @Override
        public DialectCapabilities capabilities() {
            var delegate = super.capabilities();
            return feature -> feature == SqlFeature.DML_RESULT_CLAUSE || delegate.supports(feature);
        }
    }

    private static final class NoOptimizerHintMySqlSpecs extends MySqlSpecs {
        @Override
        public DialectCapabilities capabilities() {
            var delegate = super.capabilities();
            return feature -> feature != SqlFeature.OPTIMIZER_HINT_COMMENT && delegate.supports(feature);
        }
    }
}
