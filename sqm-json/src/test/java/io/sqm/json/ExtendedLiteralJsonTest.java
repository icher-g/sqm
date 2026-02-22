package io.sqm.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtendedLiteralJsonTest {

    private final ObjectMapper mapper = SqmJsonMixins.createPretty();

    @Test
    void roundTrip_typed_literals() throws Exception {
        var query = Query.select(
            DateLiteralExpr.of("2020-01-01"),
            TimeLiteralExpr.of("10:11:12", TimeZoneSpec.WITH_TIME_ZONE),
            TimestampLiteralExpr.of("2020-01-01 00:00:00"),
            IntervalLiteralExpr.of("1", "DAY"),
            BitStringLiteralExpr.of("1010"),
            HexStringLiteralExpr.of("FF"),
            EscapeStringLiteralExpr.of("it\\'s"),
            DollarStringLiteralExpr.of("tag", "value")
        ).build();

        String json = mapper.writeValueAsString(query);
        var back = mapper.readValue(json, SelectQuery.class);

        assertEquals(query, back);
        assertTrue(json.contains("\"date-literal\""));
        assertTrue(json.contains("\"time-literal\""));
        assertTrue(json.contains("\"timestamp-literal\""));
        assertTrue(json.contains("\"interval-literal\""));
        assertTrue(json.contains("\"bit-string-literal\""));
        assertTrue(json.contains("\"hex-string-literal\""));
        assertTrue(json.contains("\"escape-string-literal\""));
        assertTrue(json.contains("\"dollar-string-literal\""));
    }
}
