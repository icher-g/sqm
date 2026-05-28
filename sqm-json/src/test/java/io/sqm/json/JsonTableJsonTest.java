package io.sqm.json;

import io.sqm.core.JsonTableBehavior;
import io.sqm.core.JsonTable;
import io.sqm.core.JsonTableScalarColumn;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonTableJsonTest {
    @Test
    void roundTripsJsonTableRef() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var table = jsonTable(
            col("payload"),
            jsonPath("$.items[*]"),
            jsonScalar(
                id("id"),
                type("NUMBER"),
                jsonPath("$.id"),
                JsonTableScalarColumn.Wrapper.WITHOUT,
                jsonBehavior(JsonTableBehavior.Kind.NULL),
                jsonBehavior(JsonTableBehavior.Kind.DEFAULT, lit("fallback"))
            ),
            jsonExists(id("present"), type("BOOLEAN"), jsonPath("$.present"), jsonBehavior(JsonTableBehavior.Kind.ERROR)),
            jsonNested(jsonPath("$.children[*]"), jsonScalar("child_id", type("NUMBER"), jsonPath("$.id"))),
            jsonOrdinality("ord")
        ).as("jt");

        var json = mapper.writeValueAsString(table);
        var roundTrip = mapper.readValue(json, JsonTable.Impl.class);

        assertEquals(table, roundTrip);
        assertTrue(json.contains("\"kind\":\"json_table\""));
        assertTrue(json.contains("\"kind\":\"DEFAULT\""));
    }
}
