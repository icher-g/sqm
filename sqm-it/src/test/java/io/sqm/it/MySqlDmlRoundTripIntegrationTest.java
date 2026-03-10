package io.sqm.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.sqm.core.Statement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.json.SqmJsonMixins;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlDmlRoundTripIntegrationTest {

    private static final ObjectMapper MAPPER = SqmJsonMixins.createDefault()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private ParseContext parseContext;
    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        parseContext = ParseContext.of(new MySqlSpecs());
        renderContext = RenderContext.of(new MySqlDialect());
    }

    @Test
    void roundTripInsertIgnoreStatement() {
        assertRoundTrip(
            "INSERT IGNORE INTO users (id, name) VALUES (1, 'alice')",
            "INSERT IGNORE INTO users (id, name) VALUES (1, 'alice')"
        );
    }

    @Test
    void roundTripReplaceIntoStatement() {
        assertRoundTrip(
            "REPLACE INTO users (id, name) VALUES (1, 'alice')",
            "REPLACE INTO users (id, name) VALUES (1, 'alice')"
        );
    }

    @Test
    void roundTripInsertOnDuplicateKeyUpdateStatement() {
        assertRoundTrip(
            "INSERT INTO users (id, name) VALUES (1, 'alice') ON DUPLICATE KEY UPDATE name = 'alice2'",
            "INSERT INTO users (id, name) VALUES (1, 'alice') ON DUPLICATE KEY UPDATE name = 'alice2'"
        );
    }

    @Test
    void roundTripJoinedUpdateStatement() {
        assertRoundTrip(
            "UPDATE users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice' WHERE orders.state = 'closed'",
            "UPDATE users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice' WHERE orders.state = 'closed'"
        );
    }

    @Test
    void roundTripJoinedUpdateWithAliasAndIndexHintsCanonicalizesOrder() {
        assertRoundTrip(
            "UPDATE users USE INDEX (idx_users_name) AS u INNER JOIN orders FORCE INDEX FOR JOIN (idx_orders_user) AS o ON u.id = o.user_id SET name = 'alice' WHERE o.state = 'closed'",
            "UPDATE users AS u USE INDEX (idx_users_name) INNER JOIN orders AS o FORCE INDEX FOR JOIN (idx_orders_user) ON u.id = o.user_id SET name = 'alice' WHERE o.state = 'closed'"
        );
    }

    @Test
    void roundTripStraightJoinedUpdateWithQualifiedAssignmentTarget() {
        assertRoundTrip(
            "UPDATE users AS u STRAIGHT_JOIN orders AS o ON u.id = o.user_id SET u.name = 'alice' WHERE o.state = 'closed'",
            "UPDATE users AS u STRAIGHT_JOIN orders AS o ON u.id = o.user_id SET u.name = 'alice' WHERE o.state = 'closed'"
        );
    }

    @Test
    void roundTripJoinedUpdateWithOptimizerHint() {
        assertRoundTrip(
            "UPDATE /*+ BKA(users) */ users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice' WHERE orders.state = 'closed'",
            "UPDATE /*+ BKA(users) */ users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice' WHERE orders.state = 'closed'"
        );
    }

    @Test
    void roundTripDeleteUsingJoinStatement() {
        assertRoundTrip(
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id WHERE orders.state = 'closed'",
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id WHERE orders.state = 'closed'"
        );
    }

    @Test
    void roundTripDeleteUsingJoinWithAliasAndIndexHintsCanonicalizesOrder() {
        assertRoundTrip(
            "DELETE FROM users USE INDEX (idx_users_name) AS u USING users USE INDEX (idx_users_name) AS u INNER JOIN orders FORCE INDEX FOR JOIN (idx_orders_user) AS o ON u.id = o.user_id WHERE o.state = 'closed'",
            "DELETE FROM users AS u USE INDEX (idx_users_name) USING users AS u USE INDEX (idx_users_name) INNER JOIN orders AS o FORCE INDEX FOR JOIN (idx_orders_user) ON u.id = o.user_id WHERE o.state = 'closed'"
        );
    }

    @Test
    void roundTripDeleteWithOptimizerHint() {
        assertRoundTrip(
            "DELETE /*+ BKA(users) */ FROM users USING users INNER JOIN orders ON users.id = orders.user_id WHERE orders.state = 'closed'",
            "DELETE /*+ BKA(users) */ FROM users USING users INNER JOIN orders ON users.id = orders.user_id WHERE orders.state = 'closed'"
        );
    }

    @Test
    void rejectsInsertIgnoreForAnsiDialect() {
        assertRejectedByAnsi(
            "INSERT IGNORE INTO users (id, name) VALUES (1, 'alice')"
        );
    }

    @Test
    void rejectsReplaceIntoForAnsiDialect() {
        var ansiParseContext = ParseContext.of(new AnsiSpecs());
        var ansiResult = ansiParseContext.parse(Statement.class, "REPLACE INTO users (id, name) VALUES (1, 'alice')");
        assertTrue(ansiResult.isError());

        var mysqlParsed = parseContext.parse(Statement.class, "REPLACE INTO users (id, name) VALUES (1, 'alice')");
        assertTrue(mysqlParsed.ok(), mysqlParsed.errorMessage());

        var ansiRenderContext = RenderContext.of(new AnsiDialect());
        assertThrows(UnsupportedDialectFeatureException.class, () -> ansiRenderContext.render(mysqlParsed.value()));
    }

    @Test
    void rejectsInsertOnDuplicateKeyUpdateForAnsiDialect() {
        assertRejectedByAnsi(
            "INSERT INTO users (id, name) VALUES (1, 'alice') ON DUPLICATE KEY UPDATE name = 'alice2'"
        );
    }

    @Test
    void rejectsJoinedUpdateForAnsiDialect() {
        assertRejectedByAnsi(
            "UPDATE users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice' WHERE orders.state = 'closed'",
            "UPDATE ... JOIN is not supported by this dialect"
        );
    }

    @Test
    void rejectsDeleteUsingJoinForAnsiDialect() {
        assertRejectedByAnsi(
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id WHERE orders.state = 'closed'",
            "DELETE ... USING is not supported by this dialect"
        );
    }

    @Test
    void rejectsPostgresOnConflictSyntax() {
        var result = parseContext.parse(Statement.class, "INSERT INTO users VALUES (1) ON CONFLICT (id) DO NOTHING");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("INSERT ... ON CONFLICT"));
    }

    private void assertRoundTrip(String originalSql, String expectedCanonicalSql) {
        var parsed = parseContext.parse(Statement.class, originalSql);
        assertTrue(parsed.ok(), parsed.errorMessage());

        var rendered = renderContext.render(parsed.value()).sql();
        assertEquals(normalize(expectedCanonicalSql), normalize(rendered));

        var reparsed = parseContext.parse(Statement.class, rendered);
        assertTrue(reparsed.ok(), reparsed.errorMessage());
        assertEquals(canonicalJson(parsed.value()), canonicalJson(reparsed.value()));
    }

    private void assertRejectedByAnsi(String mysqlSql) {
        var ansiParseContext = ParseContext.of(new AnsiSpecs());
        var ansiResult = ansiParseContext.parse(Statement.class, mysqlSql);
        assertTrue(ansiResult.isError());

        var mysqlParsed = parseContext.parse(Statement.class, mysqlSql);
        assertTrue(mysqlParsed.ok(), mysqlParsed.errorMessage());

        var ansiRenderContext = RenderContext.of(new AnsiDialect());
        assertThrows(UnsupportedDialectFeatureException.class, () -> ansiRenderContext.render(mysqlParsed.value()));
    }

    private void assertRejectedByAnsi(String mysqlSql, String expectedParseMessagePart) {
        var ansiParseContext = ParseContext.of(new AnsiSpecs());
        var ansiResult = ansiParseContext.parse(Statement.class, mysqlSql);
        assertTrue(ansiResult.isError());
        assertTrue(Objects.requireNonNull(ansiResult.errorMessage()).contains(expectedParseMessagePart));

        var mysqlParsed = parseContext.parse(Statement.class, mysqlSql);
        assertTrue(mysqlParsed.ok(), mysqlParsed.errorMessage());

        var ansiRenderContext = RenderContext.of(new AnsiDialect());
        assertThrows(UnsupportedDialectFeatureException.class, () -> ansiRenderContext.render(mysqlParsed.value()));
    }

    private static String canonicalJson(Statement statement) {
        try {
            return MAPPER.writeValueAsString(statement);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}


