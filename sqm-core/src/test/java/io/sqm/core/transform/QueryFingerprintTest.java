package io.sqm.core.transform;

import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QueryFingerprintTest {

    @Test
    void same_query_produces_same_fingerprint() {
        Query q1 = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .where(col("u", "age").gte(lit(18)));

        Query q2 = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .where(col("u", "age").gte(lit(18)));

        assertEquals(QueryFingerprint.of(q1), QueryFingerprint.of(q2));
    }

    @Test
    void queries_differing_only_by_literals_share_fingerprint_by_default() {
        Query q1 = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .where(col("u", "age").gte(lit(18)));

        Query q2 = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .where(col("u", "age").gte(lit(21)));

        assertEquals(QueryFingerprint.of(q1), QueryFingerprint.of(q2));
    }

    @Test
    void literal_sensitive_mode_distinguishes_different_literals() {
        Query q1 = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .where(col("u", "age").gte(lit(18)));

        Query q2 = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .where(col("u", "age").gte(lit(21)));

        assertNotEquals(QueryFingerprint.of(q1, false), QueryFingerprint.of(q2, false));
    }

    @Test
    void structurally_different_queries_have_different_fingerprints() {
        Query q1 = select(col("u", "id")).from(tbl("users").as("u"));
        Query q2 = select(col("u", "id")).from(tbl("orders").as("u"));

        assertNotEquals(QueryFingerprint.of(q1), QueryFingerprint.of(q2));
    }
}
