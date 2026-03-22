package io.sqm.parser.sqlserver;

import io.sqm.core.ResultClause;
import io.sqm.core.ResultItem;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultClauseParserTest {

    @Test
    void parsesOutputClauseWithoutInto() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultClause.class, "OUTPUT 1");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().items().size());
        assertNull(result.value().into());
    }

    @Test
    void parsesOutputClauseWithInsertedStar() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultClause.class, "OUTPUT inserted.*");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().items().size());
        assertEquals(
            io.sqm.core.OutputRowSource.INSERTED,
            result.value().items().getFirst().matchResultItem().outputStar(io.sqm.core.OutputStarResultItem::source).orElse(null)
        );
    }

    @Test
    void parsesOutputClauseWithAlias() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultClause.class, "OUTPUT 1 AS out_id");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("out_id", result.value().items().getFirst().matchResultItem().expr(e -> e.alias().value()).orElse(null));
    }

    @Test
    void parsesOutputClauseWithIntoWithoutColumns() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultClause.class, "OUTPUT 1 INTO my_table");

        assertTrue(result.ok(), result.errorMessage());
        assertNotNull(result.value().into());
        assertEquals(
            "my_table",
            result.value().into().target().matchTableRef().table(table -> table.name().value()).orElseThrow(AssertionError::new)
        );
        assertTrue(result.value().into().columns().isEmpty());
    }

    @Test
    void parsesOutputClauseWithIntoAndExplicitColumns() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultClause.class, "OUTPUT 1 INTO my_table (col_a, col_b)");

        assertTrue(result.ok(), result.errorMessage());
        assertNotNull(result.value().into());
        assertEquals(
            "my_table",
            result.value().into().target().matchTableRef().table(table -> table.name().value()).orElseThrow(AssertionError::new)
        );
        assertEquals(2, result.value().into().columns().size());
        assertEquals("col_a", result.value().into().columns().get(0).value());
        assertEquals("col_b", result.value().into().columns().get(1).value());
    }

    @Test
    void errorWhenIntoTargetMissing() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultClause.class, "OUTPUT 1 INTO");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }

    @Test
    void errorWhenOutputKeywordMissing() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultClause.class, "1");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }

    @Test
    void errorWhenOutputHasNoItems() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultClause.class, "OUTPUT");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }

    @Test
    void errorWhenOutputItemExpressionMissing() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultItem.class, "AS alias");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }
}
