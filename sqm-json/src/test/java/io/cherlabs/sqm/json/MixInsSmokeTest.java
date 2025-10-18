package io.cherlabs.sqm.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These tests validate:
 * - All MixIns can be registered together (no conflicts)
 * - FunctionColumn args use polymorphic typing (NAME) and round-trip
 * - We can at least serialize/deserialize minimal instances for other roots
 * <p>
 * Adjust concrete JSON payloads to match your Arg/Column/Filter shapes if needed.
 */
public class MixInsSmokeTest {

    @Test
    @DisplayName("Factory builds an ObjectMapper with all MixIns attached")
    void buildsMapper() {
        ObjectMapper m = SqmMapperFactory.createDefault();
        assertNotNull(m);
    }

    @Test
    @DisplayName("FunctionColumn args: polymorphic deserialization by NAME (e.g., 'column', 'literal')")
    void functionArgs_polymorphic_roundTrip() throws Exception {
        ObjectMapper m = SqmMapperFactory.createDefault();

        // This JSON assumes your FunctionColumnArgMixIn uses @JsonTypeInfo(use = NAME, property = "type")
        // and @JsonSubTypes names like: column, literal, star, expr (modify if yours differ).
        String json =
                """
                        {
                          "kind": "func",
                          "name": "lower",
                          "distinct": false,
                          "alias": "l",
                          "args": [
                            { "kind": "column",  "name": "name", "table": "t" },
                            { "kind": "literal", "value": "X" }
                          ]
                        }
                        """;

        // 1) Can deserialize into FunctionColumn
        io.cherlabs.sqm.core.FunctionColumn fc =
                m.readValue(json, io.cherlabs.sqm.core.FunctionColumn.class);

        assertEquals("lower", fc.name());
        assertEquals("l", fc.alias());
        assertNotNull(fc.args());
        assertEquals(2, fc.args().size());

        // 2) The Arg items are concrete subtypes; assert by class simple names to avoid package coupling
        var a0 = fc.args().get(0);
        var a1 = fc.args().get(1);
        var subtypeNames = List.of(a0.getClass().getSimpleName(), a1.getClass().getSimpleName());
        // Expect something like ColumnArg & LiteralArg
        assertTrue(subtypeNames.stream().anyMatch(s -> s.toLowerCase().contains("column")));
        assertTrue(subtypeNames.stream().anyMatch(s -> s.toLowerCase().contains("literal")));

        // 3) Round-trip back to JSON and ensure type tags are preserved
        String back = m.writeValueAsString(fc);
        assertTrue(back.contains("\"kind\":\"column\""));
        assertTrue(back.contains("\"kind\":\"literal\""));
    }

    @Test
    @DisplayName("Column MixIn: serialize a simple concrete Column")
    void column_serialization_minimal() throws Exception {
        ObjectMapper m = SqmMapperFactory.createDefault();

        // Minimal JSON for a Named/Reference column (adapt the shape to your ColumnMixIn)
        String json =
                """
                        {
                          "kind": "named",
                          "name": "u.name",
                          "alias": "n"
                        }
                        """;

        io.cherlabs.sqm.core.Column c =
                m.readValue(json, io.cherlabs.sqm.core.Column.class);

        assertNotNull(c);
        String out = m.writeValueAsString(c);
        // We don’t over-assert structure; just ensure it writes back
        assertTrue(out.contains("name"));
    }

    @Test
    @DisplayName("Filter MixIn: minimal polymorphic read/write")
    void filter_serialization_minimal() throws Exception {
        ObjectMapper m = SqmMapperFactory.createDefault();

        // Minimal JSON for a binary filter column = literal (adjust to your FilterMixIn schema)
        String json =
                """
                        {
                          "kind": "column",
                          "column": { "kind": "named", "name": "u.id" },
                          "op": "Eq",
                          "values": { "kind": "single", "value": 123 }
                        }
                        """;

        io.cherlabs.sqm.core.Filter f =
                m.readValue(json, io.cherlabs.sqm.core.Filter.class);

        assertNotNull(f);
        String out = m.writeValueAsString(f);
        assertTrue(out.contains("\"kind\""));
        assertTrue(out.contains("\"op\""));
    }

    @Test
    @DisplayName("Join MixIn: minimal read/write")
    void join_serialization_minimal() throws Exception {
        ObjectMapper m = SqmMapperFactory.createDefault();

        // Minimal JSON for a table join (adjust keys to your JoinMixIn)
        String json =
                """
                        {
                          "kind": "table",
                          "joinType": "Inner",
                          "table": { "kind": "named", "name": "users", "alias": "u" },
                          "on": {
                            "kind": "column",
                            "column":  { "kind": "named", "name": "u.id" },
                            "op": "Eq",
                            "values": {
                                "kind": "column",
                                "column": { "kind": "named", "name": "user_id", "table": "o" }
                            }
                          }
                        }
                        """;

        io.cherlabs.sqm.core.Join j =
                m.readValue(json, io.cherlabs.sqm.core.Join.class);

        assertNotNull(j);
        assertTrue(m.writeValueAsString(j).contains("\"joinType\""));
    }

    @Test
    @DisplayName("Table MixIn: minimal read/write for NamedTable")
    void table_serialization_minimal() throws Exception {
        ObjectMapper m = SqmMapperFactory.createDefault();

        String json =
                """
                        {
                          "kind": "named",
                          "name": "users",
                          "schema": "public",
                          "alias": "u"
                        }
                        """;

        io.cherlabs.sqm.core.Table t =
                m.readValue(json, io.cherlabs.sqm.core.Table.class);

        assertNotNull(t);
        assertTrue(m.writeValueAsString(t).contains("\"name\":\"users\""));
    }

    @Test
    @DisplayName("Values MixIn: minimal read/write for tuples/list")
    void values_serialization_minimal() throws Exception {
        ObjectMapper m = SqmMapperFactory.createDefault();

        // Example for tuples [[ "x", 1 ], ["y", 2 ]] — adjust to your ValuesMixIn schema
        String json =
                """
                        {
                          "kind": "tuples",
                          "rows": [
                            ["x", 1],
                            ["y", 2]
                          ]
                        }
                        """;

        io.cherlabs.sqm.core.Values v =
                m.readValue(json, io.cherlabs.sqm.core.Values.class);

        assertNotNull(v);
        String out = m.writeValueAsString(v);

        // Re-read back to a generic map to sanity-check structure survived
        Map<String, Object> map = m.readValue(out, new TypeReference<>() {
        });
        assertEquals("tuples", map.get("kind"));
    }
}
