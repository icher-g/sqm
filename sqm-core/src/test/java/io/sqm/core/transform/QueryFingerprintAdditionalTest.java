package io.sqm.core.transform;

import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryFingerprintAdditionalTest {

    @Test
    void normalize_rejects_null() {
        assertThrows(NullPointerException.class, () -> QueryFingerprint.normalize(null));
    }

    @Test
    void of_rejects_null_query() {
        assertThrows(NullPointerException.class, () -> QueryFingerprint.of(null));
        assertThrows(NullPointerException.class, () -> QueryFingerprint.of(null, false));
    }

    @Test
    void normalize_parameterizes_literals() {
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .where(col("u", "age").gte(lit(18)));

        var normalized = QueryFingerprint.normalize(query);
        assertNotNull(normalized);

        // same as default fingerprint mode.
        assertEquals(QueryFingerprint.of(query), QueryFingerprint.of(normalized, false));
    }
}
