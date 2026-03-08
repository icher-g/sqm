package io.sqm.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.sqm.core.Statement;
import io.sqm.json.SqmJsonMixins;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DmlStatementRoundTripIntegrationTest {

    private static final ObjectMapper MAPPER = SqmJsonMixins.createDefault()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private ParseContext parseContext;
    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        parseContext = ParseContext.of(new AnsiSpecs());
        renderContext = RenderContext.of(new AnsiDialect());
    }

    @Test
    void roundTripInsertValuesStatement() {
        assertRoundTrip(
            "INSERT INTO users (id, name) VALUES (1, 'alice'), (2, 'bob')",
            "INSERT INTO users (id, name) VALUES (1, 'alice'), (2, 'bob')"
        );
    }

    @Test
    void roundTripInsertSelectStatement() {
        assertRoundTrip(
            "INSERT INTO users (id) SELECT id FROM source_users",
            "INSERT INTO users (id) SELECT id FROM source_users"
        );
    }

    @Test
    void roundTripUpdateStatement() {
        assertRoundTrip(
            "UPDATE users SET name = 'alice', active = TRUE WHERE id = 1",
            "UPDATE users SET name = 'alice', active = TRUE WHERE id = 1"
        );
    }

    @Test
    void roundTripDeleteStatement() {
        assertRoundTrip(
            "DELETE FROM users WHERE id = 1",
            "DELETE FROM users WHERE id = 1"
        );
    }

    @Test
    void rejectsUnsupportedInsertSetSyntax() {
        var result = parseContext.parse(Statement.class, "INSERT INTO users SET name = 'alice'");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).startsWith("Expected SELECT or '(' to start a query"));
    }

    @Test
    void rejectsUnsupportedDeleteMultiTargetSyntax() {
        var result = parseContext.parse(Statement.class, "DELETE users FROM users");

        assertTrue(result.isError());
        assertEquals("Expected FROM after DELETE at 7", result.errorMessage());
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

