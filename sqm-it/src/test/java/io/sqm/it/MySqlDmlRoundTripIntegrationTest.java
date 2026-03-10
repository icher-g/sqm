package io.sqm.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.sqm.core.Statement;
import io.sqm.json.SqmJsonMixins;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void roundTripDeleteUsingJoinStatement() {
        assertRoundTrip(
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id WHERE orders.state = 'closed'",
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id WHERE orders.state = 'closed'"
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
