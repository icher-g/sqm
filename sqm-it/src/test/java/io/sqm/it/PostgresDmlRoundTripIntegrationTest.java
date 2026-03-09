package io.sqm.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.sqm.core.Statement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.json.SqmJsonMixins;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresDmlRoundTripIntegrationTest {

    private static final ObjectMapper MAPPER = SqmJsonMixins.createDefault()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private ParseContext parseContext;
    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        parseContext = ParseContext.of(new PostgresSpecs());
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    void roundTripInsertReturningStatement() {
        assertRoundTrip(
            "INSERT INTO users (name) VALUES ('alice') RETURNING id, name",
            "INSERT INTO users (name) VALUES ('alice') RETURNING id, name"
        );
    }

    @Test
    void roundTripInsertOnConflictDoNothingStatement() {
        assertRoundTrip(
            "INSERT INTO users (id, name) VALUES (1, 'alice') ON CONFLICT (id) DO NOTHING",
            "INSERT INTO users (id, name) VALUES (1, 'alice') ON CONFLICT (id) DO NOTHING"
        );
    }

    @Test
    void roundTripInsertOnConflictDoUpdateStatement() {
        assertRoundTrip(
            "INSERT INTO users (id, name) VALUES (1, 'alice') ON CONFLICT (id) DO UPDATE SET name = 'alice2' WHERE id = 1",
            "INSERT INTO users (id, name) VALUES (1, 'alice') ON CONFLICT (id) DO UPDATE SET name = 'alice2' WHERE id = 1"
        );
    }

    @Test
    void roundTripUpdateFromStatement() {
        assertRoundTrip(
            "UPDATE users SET name = src.name FROM source_users AS src WHERE users.id = src.id",
            "UPDATE users SET name = src.name FROM source_users AS src WHERE users.id = src.id"
        );
    }

    @Test
    void roundTripUpdateReturningStatement() {
        assertRoundTrip(
            "UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id, name",
            "UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id, name"
        );
    }

    @Test
    void roundTripDeleteUsingStatement() {
        assertRoundTrip(
            "DELETE FROM users USING source_users AS src WHERE users.id = src.id",
            "DELETE FROM users USING source_users AS src WHERE users.id = src.id"
        );
    }

    @Test
    void roundTripDeleteReturningStatement() {
        assertRoundTrip(
            "DELETE FROM users WHERE id = 1 RETURNING id, name",
            "DELETE FROM users WHERE id = 1 RETURNING id, name"
        );
    }

    @Test
    void roundTripWritableCteInsertReturningStatement() {
        assertRoundTrip(
            "WITH ins AS ( INSERT INTO users (name) VALUES ('alice') RETURNING id ) SELECT id FROM ins",
            "WITH ins AS ( INSERT INTO users (name) VALUES ('alice') RETURNING id ) SELECT id FROM ins"
        );
    }

    @Test
    void roundTripWritableCteUpdateReturningStatement() {
        assertRoundTrip(
            "WITH upd AS ( UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id ) SELECT id FROM upd",
            "WITH upd AS ( UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id ) SELECT id FROM upd"
        );
    }

    @Test
    void roundTripWritableCteDeleteReturningStatement() {
        assertRoundTrip(
            "WITH del AS ( DELETE FROM users WHERE id = 1 RETURNING id ) SELECT id FROM del",
            "WITH del AS ( DELETE FROM users WHERE id = 1 RETURNING id ) SELECT id FROM del"
        );
    }

    @Test
    void rejectsInsertReturningForAnsiDialect() {
        assertRejectedByAnsi(
            "INSERT INTO users (name) VALUES ('alice') RETURNING id",
            "INSERT ... RETURNING is not supported by this dialect"
        );
    }

    @Test
    void rejectsInsertOnConflictForAnsiDialect() {
        assertRejectedByAnsi(
            "INSERT INTO users (id, name) VALUES (1, 'alice') ON CONFLICT (id) DO NOTHING",
            "INSERT ... ON CONFLICT is not supported by this dialect"
        );
    }

    @Test
    void rejectsUpdateFromForAnsiDialect() {
        assertRejectedByAnsi(
            "UPDATE users SET name = src.name FROM source_users AS src WHERE users.id = src.id",
            "UPDATE ... FROM is not supported by this dialect"
        );
    }

    @Test
    void rejectsUpdateReturningForAnsiDialect() {
        assertRejectedByAnsi(
            "UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id",
            "UPDATE ... RETURNING is not supported by this dialect"
        );
    }

    @Test
    void rejectsDeleteUsingForAnsiDialect() {
        assertRejectedByAnsi(
            "DELETE FROM users USING source_users AS src WHERE users.id = src.id",
            "DELETE ... USING is not supported by this dialect"
        );
    }

    @Test
    void rejectsDeleteReturningForAnsiDialect() {
        assertRejectedByAnsi(
            "DELETE FROM users WHERE id = 1 RETURNING id",
            "DELETE ... RETURNING is not supported by this dialect"
        );
    }

    @Test
    void rejectsWritableCteInsertReturningForAnsiDialect() {
        var postgresSql = "WITH ins AS ( INSERT INTO users (name) VALUES ('alice') RETURNING id ) SELECT id FROM ins";

        var ansiParseContext = ParseContext.of(new AnsiSpecs());
        var ansiResult = ansiParseContext.parse(Statement.class, postgresSql);
        assertTrue(ansiResult.isError());

        var pgParsed = parseContext.parse(Statement.class, postgresSql);
        assertTrue(pgParsed.ok(), pgParsed.errorMessage());

        var ansiRenderContext = RenderContext.of(new AnsiDialect());
        assertThrows(UnsupportedDialectFeatureException.class, () -> ansiRenderContext.render(pgParsed.value()));
    }

    @Test
    void rejectsWritableCteUpdateReturningForAnsiDialect() {
        var postgresSql = "WITH upd AS ( UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id ) SELECT id FROM upd";

        var ansiParseContext = ParseContext.of(new AnsiSpecs());
        var ansiResult = ansiParseContext.parse(Statement.class, postgresSql);
        assertTrue(ansiResult.isError());

        var pgParsed = parseContext.parse(Statement.class, postgresSql);
        assertTrue(pgParsed.ok(), pgParsed.errorMessage());

        var ansiRenderContext = RenderContext.of(new AnsiDialect());
        assertThrows(UnsupportedDialectFeatureException.class, () -> ansiRenderContext.render(pgParsed.value()));
    }

    @Test
    void rejectsWritableCteDeleteReturningForAnsiDialect() {
        var postgresSql = "WITH del AS ( DELETE FROM users WHERE id = 1 RETURNING id ) SELECT id FROM del";

        var ansiParseContext = ParseContext.of(new AnsiSpecs());
        var ansiResult = ansiParseContext.parse(Statement.class, postgresSql);
        assertTrue(ansiResult.isError());

        var pgParsed = parseContext.parse(Statement.class, postgresSql);
        assertTrue(pgParsed.ok(), pgParsed.errorMessage());

        var ansiRenderContext = RenderContext.of(new AnsiDialect());
        assertThrows(UnsupportedDialectFeatureException.class, () -> ansiRenderContext.render(pgParsed.value()));
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

    private void assertRejectedByAnsi(String postgresSql, String expectedParseMessagePart) {
        var ansiParseContext = ParseContext.of(new AnsiSpecs());
        var ansiResult = ansiParseContext.parse(Statement.class, postgresSql);
        assertTrue(ansiResult.isError());
        assertTrue(Objects.requireNonNull(ansiResult.errorMessage()).contains(expectedParseMessagePart));

        var pgParsed = parseContext.parse(Statement.class, postgresSql);
        assertTrue(pgParsed.ok(), pgParsed.errorMessage());

        var ansiRenderContext = RenderContext.of(new AnsiDialect());
        assertThrows(UnsupportedDialectFeatureException.class, () -> ansiRenderContext.render(pgParsed.value()));
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
