package io.sqm.json;

import io.sqm.core.JsonTableRef;
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
            jsonScalar("id", type("NUMBER"), jsonPath("$.id")), jsonOrdinality("ord")
        ).as("jt");

        var json = mapper.writeValueAsString(table);
        var roundTrip = mapper.readValue(json, JsonTableRef.Impl.class);

        assertEquals(table, roundTrip);
        assertTrue(json.contains("\"kind\":\"json_table\""));
    }
}
