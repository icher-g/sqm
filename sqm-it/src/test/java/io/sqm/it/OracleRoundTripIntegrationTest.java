package io.sqm.it;

import io.sqm.core.Query;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration coverage for the baseline Oracle query surface.
 */
class OracleRoundTripIntegrationTest {

    @Test
    void roundTrip_quotedIdentifiersAndFetchPagination() {
        String sql = """
            SELECT "u"."id", "u"."name"
            FROM "users" AS "u"
            ORDER BY "u"."id"
            OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY
            """.trim();

        Query parsed = Utils.parseOracle(sql);
        String rendered = Utils.renderOracle(parsed);
        Query reparsed = Utils.parseOracle(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT \"u\".\"id\", \"u\".\"name\" FROM \"users\" \"u\" "
                + "ORDER BY \"u\".\"id\" OFFSET 5 ROWS FETCH FIRST 10 ROWS ONLY",
            rendered
        );
    }

    @Test
    void roundTrip_lateralInlineView() {
        String sql = """
            SELECT u.id
            FROM users AS u
            CROSS JOIN LATERAL (SELECT id FROM orders WHERE user_id = u.id) AS o
            """.trim();

        Query parsed = Utils.parseOracle(sql);
        String rendered = Utils.renderOracle(parsed);
        Query reparsed = Utils.parseOracle(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT u.id FROM users u CROSS JOIN LATERAL "
                + "( SELECT id FROM orders WHERE user_id = u.id ) o",
            rendered
        );
    }

    @Test
    void rejectsLimitSyntaxThatOracleDoesNotSupport() {
        var result = ParseContext.of(new OracleSpecs()).parse(Query.class, "SELECT id FROM users LIMIT 10");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("LIMIT"));
    }
}
