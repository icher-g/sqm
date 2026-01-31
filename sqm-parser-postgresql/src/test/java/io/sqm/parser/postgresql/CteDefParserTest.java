package io.sqm.parser.postgresql;

import io.sqm.core.CteDef;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}
