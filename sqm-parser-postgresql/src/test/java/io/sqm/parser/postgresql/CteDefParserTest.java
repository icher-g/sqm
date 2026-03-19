package io.sqm.parser.postgresql;

import io.sqm.core.CteDef;
import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CteDefParserTest {

    private final ParseContext ctx = ParseContext.of(new PostgresSpecs());
    private final CteDefParser parser = new CteDefParser();

    private ParseResult<? extends CteDef> parse(String sql) {
        return ctx.parse(parser, sql);
    }

    @Test
    @DisplayName("Parses MATERIALIZED")
    void parses_materialized() {
        var res = parse("t AS MATERIALIZED (SELECT 1)");
        assertTrue(res.ok());
        assertEquals(CteDef.Materialization.MATERIALIZED, res.value().materialization());
    }

    @Test
    @DisplayName("Parses NOT MATERIALIZED")
    void parses_not_materialized() {
        var res = parse("t AS NOT MATERIALIZED (SELECT 1)");
        assertTrue(res.ok());
        assertEquals(CteDef.Materialization.NOT_MATERIALIZED, res.value().materialization());
    }

    @Test
    @DisplayName("Rejects NOT without MATERIALIZED")
    void rejects_not_without_materialized() {
        var res = parse("t AS NOT (SELECT 1)");
        assertTrue(res.isError());
    }

    @Test
    @DisplayName("Parses writable INSERT CTE with RETURNING")
    void parses_writable_insert_cte_with_returning() {
        var res = parse("ins AS (INSERT INTO users (name) VALUES ('alice') RETURNING id)");
        assertTrue(res.ok(), () -> String.valueOf(res.errorMessage()));

        var body = res.value().body();
        assertInstanceOf(InsertStatement.class, body);
        assertEquals(1, ((InsertStatement) body).result().items().size());
    }

    @Test
    @DisplayName("Parses writable UPDATE CTE with RETURNING")
    void parses_writable_update_cte_with_returning() {
        var res = parse("upd AS (UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id)");
        assertTrue(res.ok(), () -> String.valueOf(res.errorMessage()));

        var body = res.value().body();
        assertInstanceOf(UpdateStatement.class, body);
        assertEquals(1, ((UpdateStatement) body).result().items().size());
    }

    @Test
    @DisplayName("Parses writable DELETE CTE with RETURNING")
    void parses_writable_delete_cte_with_returning() {
        var res = parse("del AS (DELETE FROM users WHERE id = 1 RETURNING id)");
        assertTrue(res.ok(), () -> String.valueOf(res.errorMessage()));

        var body = res.value().body();
        assertInstanceOf(DeleteStatement.class, body);
        assertEquals(1, ((DeleteStatement) body).result().items().size());
    }

    @Test
    @DisplayName("Rejects writable INSERT CTE without RETURNING")
    void rejects_writable_insert_cte_without_returning() {
        var res = parse("ins AS (INSERT INTO users (name) VALUES ('alice'))");
        assertTrue(res.isError());
        assertTrue(String.valueOf(res.errorMessage()).contains("requires RETURNING"));
    }

    @Test
    @DisplayName("Rejects writable UPDATE CTE without RETURNING")
    void rejects_writable_update_cte_without_returning() {
        var res = parse("upd AS (UPDATE users SET name = 'alice' WHERE id = 1)");
        assertTrue(res.isError());
        assertTrue(String.valueOf(res.errorMessage()).contains("requires RETURNING"));
    }

    @Test
    @DisplayName("Rejects writable DELETE CTE without RETURNING")
    void rejects_writable_delete_cte_without_returning() {
        var res = parse("del AS (DELETE FROM users WHERE id = 1)");
        assertTrue(res.isError());
        assertTrue(String.valueOf(res.errorMessage()).contains("requires RETURNING"));
    }

    @Test
    @DisplayName("Supports writable INSERT CTE through statement entrypoint")
    void parses_writable_insert_cte_through_statement_entrypoint() {
        var statement = ctx.parse(Statement.class,
            "WITH ins AS (INSERT INTO users (name) VALUES ('alice') RETURNING id) SELECT id FROM ins");
        assertTrue(statement.ok(), () -> String.valueOf(statement.errorMessage()));
    }

    @Test
    @DisplayName("Supports writable UPDATE CTE through statement entrypoint")
    void parses_writable_update_cte_through_statement_entrypoint() {
        var statement = ctx.parse(Statement.class,
            "WITH upd AS (UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id) SELECT id FROM upd");
        assertTrue(statement.ok(), () -> String.valueOf(statement.errorMessage()));
    }

    @Test
    @DisplayName("Supports writable DELETE CTE through statement entrypoint")
    void parses_writable_delete_cte_through_statement_entrypoint() {
        var statement = ctx.parse(Statement.class,
            "WITH del AS (DELETE FROM users WHERE id = 1 RETURNING id) SELECT id FROM del");
        assertTrue(statement.ok(), () -> String.valueOf(statement.errorMessage()));
    }
}
