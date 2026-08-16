package io.sqm.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.sqm.core.Statement;
import io.sqm.json.SqmJsonMixins;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration coverage for Oracle DML and Oracle-specific result targets.
 */
class OracleDmlRoundTripIntegrationTest {

    private static final ObjectMapper MAPPER = SqmJsonMixins.createDefault()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private ParseContext parseContext;
    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        parseContext = ParseContext.of(new OracleSpecs());
        renderContext = RenderContext.of(new OracleDialect());
    }

    @Test
    void roundTripDmlWithReturningInto() {
        assertRoundTrip(
            "INSERT INTO users (id, name) VALUES (1, 'alice') RETURNING id, name INTO :id, :name",
            "INSERT INTO users (id, name) VALUES (1, 'alice') RETURNING id, name INTO :id, :name"
        );
        assertRoundTrip(
            "UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id INTO :id",
            "UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id INTO :id"
        );
        assertRoundTrip(
            "DELETE FROM users WHERE id = 1 RETURNING id INTO :id",
            "DELETE FROM users WHERE id = 1 RETURNING id INTO :id"
        );
    }

    @Test
    void roundTripBaselineMerge() {
        assertRoundTrip(
            """
                MERGE INTO users USING src_users AS s ON users.id = s.id
                WHEN MATCHED THEN UPDATE SET name = s.name
                WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name)
                """,
            """
                MERGE INTO users USING src_users s ON (users.id = s.id)
                WHEN MATCHED THEN UPDATE SET name = s.name
                WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name)
                """
        );
    }

    @Test
    void rejectsGenericReturningWithoutOracleVariableTargets() {
        var result = parseContext.parse(Statement.class, "INSERT INTO users (id) VALUES (1) RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("RETURNING"));
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
