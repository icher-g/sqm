package io.sqm.parser.sqlserver;

import io.sqm.core.ResultInto;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ResultIntoParserTest {

    @Test
    void parsesIntoTargetWithoutColumns() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultInto.class, "INTO my_table");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(
            "my_table",
            result.value().target().matchTableRef().table(table -> table.name().value()).orElseThrow(AssertionError::new)
        );
        assertTrue(result.value().columns().isEmpty());
    }

    @Test
    void parsesIntoTargetWithColumns() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultInto.class, "INTO my_table (col_a, col_b)");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(
            "my_table",
            result.value().target().matchTableRef().table(table -> table.name().value()).orElseThrow(AssertionError::new)
        );
        assertEquals(2, result.value().columns().size());
    }

    @Test
    void parsesIntoTableVariableTarget() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultInto.class, "INTO @audit (col_a, col_b)");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(
            "audit",
            result.value().target().matchTableRef().variableTable(variable -> variable.name().value()).orElseThrow(AssertionError::new)
        );
        assertEquals(2, result.value().columns().size());
    }

    @Test
    void errorWhenIntoTargetMissing() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultInto.class, "INTO");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }

    @Test
    void errorWhenOutputIntoColumnsUnterminated() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultInto.class, "INTO my_table (col_a, col_b");

        assertTrue(result.isError());
        assertNotNull(result.errorMessage());
    }

    @Test
    void errorWhenOutputIntoTargetContainsTableHints() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(ResultInto.class, "INTO audit WITH (NOLOCK)");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("table hints"));
    }
}
