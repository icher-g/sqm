package io.sqm.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.sqm.core.Statement;
import io.sqm.json.SqmJsonMixins;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
