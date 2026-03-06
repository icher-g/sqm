package io.sqm.it;

import io.sqm.core.Query;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlRoundTripIntegrationTest {

    @Test
    void roundTrip_select_with_group_order_limit_offset() {
        String sql = """
            SELECT u.id, u.name, COUNT(*) AS cnt
            FROM users AS u
            WHERE u.active = TRUE
            GROUP BY u.id, u.name
            ORDER BY u.name
            LIMIT 10 OFFSET 20
            """.trim();

        Query parsed = Utils.parseMySql(sql);
        String rendered = Utils.renderMySql(parsed);
        Query reparsed = Utils.parseMySql(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals("SELECT u.id, u.name, COUNT(*) AS cnt FROM users AS u WHERE u.active = TRUE GROUP BY u.id, u.name ORDER BY u.name LIMIT 10 OFFSET 20", Utils.normalizeSql(rendered));
    }

    @Test
    void roundTrip_limit_comma_form_is_canonicalized() {
        String sql = "SELECT id FROM users LIMIT 5, 10";

        Query parsed = Utils.parseMySql(sql);
        String rendered = Utils.renderMySql(parsed);
        Query reparsed = Utils.parseMySql(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals("SELECT id FROM users LIMIT 10 OFFSET 5", Utils.normalizeSql(rendered));
    }

    @Test
    void roundTrip_preserves_backtick_identifiers() {
        String sql = "SELECT `u`.`id`, `u`.`name` FROM `users` AS `u` LIMIT 1";

        Query parsed = Utils.parseMySql(sql);
        String rendered = Utils.renderMySql(parsed);

        assertEquals("SELECT `u`.`id`, `u`.`name` FROM `users` AS `u` LIMIT 1", Utils.normalizeSql(rendered));
    }

    @Test
    void roundTrip_nullSafeEqualityPredicate() {
        String sql = "SELECT id FROM users WHERE deleted_at <=> NULL";

        Query parsed = Utils.parseMySql(sql);
        String rendered = Utils.renderMySql(parsed);
        Query reparsed = Utils.parseMySql(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals("SELECT id FROM users WHERE deleted_at <=> NULL", Utils.normalizeSql(rendered));
    }

    @Test
    void roundTrip_mysqlRlikeCanonicalizesToRegexp() {
        String sql = "SELECT id FROM users WHERE name RLIKE '^a'";

        Query parsed = Utils.parseMySql(sql);
        String rendered = Utils.renderMySql(parsed);
        Query reparsed = Utils.parseMySql(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals("SELECT id FROM users WHERE name REGEXP '^a'", Utils.normalizeSql(rendered));
    }

    @Test
    void roundTrip_forShareSkipLocked() {
        String sql = "SELECT id FROM jobs FOR SHARE SKIP LOCKED";

        Query parsed = Utils.parseMySql(sql);
        String rendered = Utils.renderMySql(parsed);
        Query reparsed = Utils.parseMySql(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals("SELECT id FROM jobs FOR SHARE SKIP LOCKED", Utils.normalizeSql(rendered));
    }

    @Test
    void roundTrip_groupByWithRollupCanonicalForm() {
        String sql = "SELECT department, status FROM employees GROUP BY department, status WITH ROLLUP";

        Query parsed = Utils.parseMySql(sql);
        String rendered = Utils.renderMySql(parsed);
        Query reparsed = Utils.parseMySql(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals("SELECT department, status FROM employees GROUP BY department, status WITH ROLLUP", Utils.normalizeSql(rendered));
    }

    @Test
    void parser_rejects_distinct_on() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Query.class, "SELECT DISTINCT ON (id) id FROM users");

        assertTrue(result.isError());
    }

    @Test
    void parser_rejects_postgres_typecast_operator() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Query.class, "SELECT 1::int");

        assertTrue(result.isError());
    }
}


