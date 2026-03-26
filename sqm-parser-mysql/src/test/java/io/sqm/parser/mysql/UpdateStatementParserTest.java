package io.sqm.parser.mysql;

import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
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

class UpdateStatementParserTest {

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
        assertEquals(1, result.value().table().hints().size());
        assertEquals(1, result.value().joins().size());
    }

    @Test
    void parsesOptimizerHintAfterUpdateKeyword() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE /*+ BKA(users) */ users SET name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(java.util.List.of("BKA"), result.value().hints().stream().map(h -> h.name().value()).toList());
    }

    @Test
    void parsesJoinedUpdateWithQualifiedAssignmentTarget() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE users AS u INNER JOIN orders AS o ON u.id = o.user_id SET u.name = 'alice' WHERE o.state = 'closed'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(java.util.List.of("u", "name"), result.value().assignments().getFirst().column().values());
    }

    @Test
    void parsesStraightJoinedUpdateStatement() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE users AS u STRAIGHT_JOIN orders AS o ON u.id = o.user_id SET u.name = 'alice' WHERE o.state = 'closed'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(io.sqm.core.JoinKind.STRAIGHT, ((io.sqm.core.OnJoin) result.value().joins().getFirst()).kind());
        assertEquals(java.util.List.of("u", "name"), result.value().assignments().getFirst().column().values());
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
    void parsesUpdateReturningWhenCapabilityIsEnabled() {
        var ctx = ParseContext.of(new ReturningMySqlSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice' RETURNING id");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().result().items().size());
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
    void parserRejectsStraightJoinedUpdateWhenCapabilityIsMissing() {
        var ctx = ParseContext.of(new NoStraightJoinMySqlSpecs());
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE users AS u STRAIGHT_JOIN orders AS o ON u.id = o.user_id SET u.name = 'alice'");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("STRAIGHT_JOIN"));
    }

    @Test
    void parserRejectsUpdateOptimizerHintWhenCapabilityIsMissing() {
        var ctx = ParseContext.of(new NoOptimizerHintMySqlSpecs());
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE /*+ BKA(users) */ users SET name = 'alice'");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Optimizer hint comments"));
    }

    @Test
    void rejectsUpdateReturningInMysql57() {
        var ctx = ParseContext.of(new MySqlSpecs(SqlDialectVersion.of(5, 7)));
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice' RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("UPDATE ... RETURNING"));
    }

    private static final class ReturningMySqlSpecs extends MySqlSpecs {
        @Override
        public DialectCapabilities capabilities() {
            var delegate = super.capabilities();
            return feature -> feature == SqlFeature.DML_RESULT_CLAUSE || delegate.supports(feature);
        }
    }

    private static final class NoStraightJoinMySqlSpecs extends MySqlSpecs {
        @Override
        public DialectCapabilities capabilities() {
            var delegate = super.capabilities();
            return feature -> feature != SqlFeature.STRAIGHT_JOIN && delegate.supports(feature);
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
