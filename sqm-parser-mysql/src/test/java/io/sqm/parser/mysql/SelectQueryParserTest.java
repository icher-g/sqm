package io.sqm.parser.mysql;

import io.sqm.core.Query;
import io.sqm.core.SelectModifier;
import io.sqm.core.SelectQuery;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectQueryParserTest {

    @Test
    void parsesSqlCalcFoundRowsModifier() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Query.class, "SELECT SQL_CALC_FOUND_ROWS id FROM users");

        assertTrue(result.ok());
        var select = (SelectQuery) result.value();
        assertEquals(1, select.modifiers().size());
        assertEquals(SelectModifier.CALC_FOUND_ROWS, select.modifiers().getFirst());
    }

    @Test
    void parsesOptimizerHintCommentAfterSelect() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Query.class, "SELECT /*+ NO_RANGE_OPTIMIZATION(users) */ id FROM users");

        assertTrue(result.ok());
        var select = (SelectQuery) result.value();
        assertEquals(1, select.hints().size());
        assertEquals("NO_RANGE_OPTIMIZATION", select.hints().getFirst().name().value());
    }

    @Test
    void parsesOptimizerHintCommentBeforeSelect() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Query.class, "/*+ MAX_EXECUTION_TIME(1000) */ SELECT id FROM users");

        assertTrue(result.ok());
        var select = (SelectQuery) result.value();
        assertEquals(1, select.hints().size());
        assertEquals("MAX_EXECUTION_TIME", select.hints().getFirst().name().value());
    }

    @Test
    void parsesMultipleOptimizerHintsAndCalcFoundRows() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Query.class,
            "/*+ SET_VAR(sort_buffer_size=16M) */ SELECT /*+ BKA(users) */ SQL_CALC_FOUND_ROWS id FROM users");

        assertTrue(result.ok());
        var select = (SelectQuery) result.value();
        assertEquals(2, select.hints().size());
        assertEquals("SET_VAR", select.hints().get(0).name().value());
        assertEquals("BKA", select.hints().get(1).name().value());
        assertEquals(SelectModifier.CALC_FOUND_ROWS, select.modifiers().getFirst());
    }

    @Test
    void parsesStraightJoin() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Query.class,
            "SELECT u.id FROM users AS u STRAIGHT_JOIN orders AS o ON u.id = o.user_id");

        assertTrue(result.ok(), result.errorMessage());
        var select = (SelectQuery) result.value();
        assertEquals(1, select.joins().size());
        assertEquals(io.sqm.core.JoinKind.STRAIGHT, ((io.sqm.core.OnJoin) select.joins().getFirst()).kind());
    }

    @Test
    void parserRejectsSqlCalcFoundRowsWhenCapabilityIsMissing() {
        var parser = new SelectQueryParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("SELECT SQL_CALC_FOUND_ROWS id FROM users", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("SQL_CALC_FOUND_ROWS"));
    }

    @Test
    void parserRejectsOptimizerHintWhenCapabilityIsMissing() {
        var parser = new SelectQueryParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("SELECT /*+ BKA(users) */ id FROM users", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Optimizer hint comments"));
    }

    @Test
    void parserRejectsLeadingOptimizerHintWhenCapabilityIsMissing() {
        var parser = new SelectQueryParser();
        var ctx = ParseContext.of(new AnsiSpecs());
        var cur = Cursor.of("/*+ BKA(users) */ SELECT id FROM users", ctx.identifierQuoting());

        var result = parser.parse(cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Optimizer hint comments"));
    }

    @Test
    void parserRejectsStraightJoinWhenCapabilityIsMissing() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Query.class, "SELECT u.id FROM users AS u STRAIGHT_JOIN orders AS o ON u.id = o.user_id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("STRAIGHT_JOIN"));
    }
}
